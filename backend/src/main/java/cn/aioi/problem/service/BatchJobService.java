package cn.aioi.problem.service;

import cn.aioi.problem.ai.AiAssessment;
import cn.aioi.problem.ai.AiProvider;
import cn.aioi.problem.ai.ProblemInput;
import cn.aioi.problem.api.dto.BatchDtos;
import cn.aioi.problem.domain.BatchItemStatus;
import cn.aioi.problem.domain.BatchJob;
import cn.aioi.problem.domain.BatchJobItem;
import cn.aioi.problem.domain.BatchJobStatus;
import cn.aioi.problem.domain.Problem;
import cn.aioi.problem.domain.User;
import cn.aioi.problem.repository.BatchJobItemRepository;
import cn.aioi.problem.repository.BatchJobRepository;
import cn.aioi.problem.repository.ProblemRepository;
import cn.aioi.problem.repository.UserRepository;
import jakarta.annotation.PostConstruct;
import jakarta.annotation.PreDestroy;
import jakarta.persistence.EntityNotFoundException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;
import org.springframework.transaction.support.TransactionTemplate;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicBoolean;

@Service
public class BatchJobService {
    private final BatchJobRepository jobs;
    private final BatchJobItemRepository items;
    private final ProblemRepository problems;
    private final UserRepository users;
    private final AiProvider aiProvider;
    private final TransactionTemplate transactionTemplate;
    private final ExecutorService executor = Executors.newSingleThreadExecutor();
    private final AtomicBoolean workerRunning = new AtomicBoolean(false);

    public BatchJobService(BatchJobRepository jobs, BatchJobItemRepository items, ProblemRepository problems,
                           UserRepository users, AiProvider aiProvider, PlatformTransactionManager transactionManager) {
        this.jobs = jobs;
        this.items = items;
        this.problems = problems;
        this.users = users;
        this.aiProvider = aiProvider;
        this.transactionTemplate = new TransactionTemplate(transactionManager);
    }

    @PostConstruct
    void resumePendingWorkOnStartup() {
        triggerWorker();
    }

    @Transactional
    public BatchDtos.BatchJobDetailResponse upload(String name, MultipartFile[] files, User user) {
        if (files == null || files.length == 0) {
            throw new IllegalArgumentException("请选择至少一个 .txt 或 .md 文件");
        }
        if (files.length > 5000) {
            throw new IllegalArgumentException("单次最多上传 5000 个文件");
        }
        BatchJob job = jobs.save(new BatchJob(defaultName(name), user, files.length));
        for (MultipartFile file : files) {
            if (file.isEmpty()) {
                throw new IllegalArgumentException("文件不能为空：" + file.getOriginalFilename());
            }
            String filename = file.getOriginalFilename() == null ? "未命名.txt" : file.getOriginalFilename();
            if (!isTextProblemFile(filename)) {
                throw new IllegalArgumentException("仅支持 .txt 或 .md 文件：" + filename);
            }
            items.save(new BatchJobItem(job, titleFromFilename(filename), readUtf8(file), (int) items.countByJobAndStatus(job, BatchItemStatus.PENDING)));
        }
        triggerWorkerAfterCommit();
        return detail(job.getId(), user);
    }

    @Transactional(readOnly = true)
    public List<BatchDtos.BatchJobResponse> list(User user) {
        return jobs.findByOwnerOrderByCreatedAtDesc(user).stream()
                .map(this::toJobResponse)
                .toList();
    }

    @Transactional(readOnly = true)
    public BatchDtos.BatchJobDetailResponse detail(Long id, User user) {
        BatchJob job = ownedJob(id, user);
        return new BatchDtos.BatchJobDetailResponse(
                toJobResponse(job),
                items.findByJobOrderBySortOrderAscIdAsc(job).stream()
                        .map(this::toItemResponse)
                        .toList()
        );
    }

    @Transactional
    public BatchDtos.BatchJobResponse pause(Long id, User user) {
        BatchJob job = ownedJob(id, user);
        job.pause();
        return toJobResponse(job);
    }

    @Transactional
    public BatchDtos.BatchJobResponse resume(Long id, User user) {
        BatchJob job = ownedJob(id, user);
        job.resume();
        triggerWorkerAfterCommit();
        return toJobResponse(job);
    }

    @Transactional
    public BatchDtos.BatchItemResponse updateItem(Long jobId, Long itemId, BatchDtos.BatchItemUpdateRequest request, User user) {
        BatchJob job = ownedJob(jobId, user);
        BatchJobItem item = ownedItem(job, itemId);
        item.updatePending(request.title().trim(), request.content().trim());
        return toItemResponse(item);
    }

    @Transactional
    public BatchDtos.BatchJobDetailResponse deleteItem(Long jobId, Long itemId, User user) {
        BatchJob job = ownedJob(jobId, user);
        BatchJobItem item = ownedItem(job, itemId);
        if (item.getStatus() != BatchItemStatus.PENDING) {
            throw new IllegalStateException("只有等待中的任务可以删除");
        }
        items.delete(item);
        job.decrementTotalCount();
        completeJobIfDone(job);
        return detail(jobId, user);
    }

    @Transactional
    public BatchDtos.BatchJobDetailResponse reorderItems(Long jobId, BatchDtos.BatchItemReorderRequest request, User user) {
        BatchJob job = ownedJob(jobId, user);
        Set<Long> seen = new HashSet<>();
        int index = 0;
        for (Long itemId : request.itemIds()) {
            if (!seen.add(itemId)) {
                throw new IllegalArgumentException("任务排序列表包含重复项");
            }
            BatchJobItem item = ownedItem(job, itemId);
            item.setSortOrder(index++);
        }
        return detail(jobId, user);
    }

    public void triggerWorker() {
        if (workerRunning.compareAndSet(false, true)) {
            executor.submit(this::workerLoop);
        }
    }

    private void triggerWorkerAfterCommit() {
        if (TransactionSynchronizationManager.isSynchronizationActive()) {
            TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
                @Override
                public void afterCommit() {
                    triggerWorker();
                }
            });
        } else {
            triggerWorker();
        }
    }

    private void workerLoop() {
        try {
            while (true) {
                Optional<Long> nextItemId = transactionTemplate.execute(status -> nextPendingItemId());
                if (nextItemId == null || nextItemId.isEmpty()) {
                    return;
                }
                processItem(nextItemId.get());
            }
        } finally {
            workerRunning.set(false);
            Boolean hasMore = transactionTemplate.execute(status -> jobs.findByStatusOrderByCreatedAtAsc(BatchJobStatus.RUNNING).stream()
                    .anyMatch(job -> items.countByJobAndStatus(job, BatchItemStatus.PENDING) > 0));
            if (Boolean.TRUE.equals(hasMore)) {
                triggerWorker();
            }
        }
    }

    private Optional<Long> nextPendingItemId() {
        for (BatchJob job : jobs.findByStatusOrderByCreatedAtAsc(BatchJobStatus.RUNNING)) {
            Optional<BatchJobItem> item = items.findFirstByJobAndStatusOrderBySortOrderAscIdAsc(job, BatchItemStatus.PENDING);
            if (item.isPresent()) {
                item.get().start();
                return Optional.of(item.get().getId());
            }
            if (items.countByJobAndStatus(job, BatchItemStatus.RUNNING) == 0) {
                job.complete();
            }
        }
        return Optional.empty();
    }

    private void processItem(Long itemId) {
        WorkItem workItem = transactionTemplate.execute(status -> {
            BatchJobItem item = items.findById(itemId).orElseThrow();
            return new WorkItem(item.getId(), item.getJob().getId(), item.getJob().getOwner().getId(), item.getTitle(), item.getContent());
        });
        if (workItem == null) {
            return;
        }
        try {
            AiAssessment assessment = aiProvider.assess(new ProblemInput(workItem.title(), workItem.content()));
            transactionTemplate.executeWithoutResult(status -> {
                BatchJobItem item = items.findById(workItem.itemId()).orElseThrow();
                User owner = users.getReferenceById(workItem.ownerId());
                Problem problem = problems.save(new Problem(
                        workItem.title(),
                        workItem.content(),
                        assessment.difficulty(),
                        new LinkedHashSet<>(assessment.tags()),
                        "批量导入",
                        owner
                ));
                item.succeed(problem.getId());
                item.getJob().incrementSuccess();
                completeJobIfDone(item.getJob());
            });
        } catch (RuntimeException exception) {
            transactionTemplate.executeWithoutResult(status -> {
                BatchJobItem item = items.findById(workItem.itemId()).orElseThrow();
                item.fail(exception.getMessage());
                item.getJob().incrementFailed();
                completeJobIfDone(item.getJob());
            });
        }
    }

    private void completeJobIfDone(BatchJob job) {
        long pending = items.countByJobAndStatus(job, BatchItemStatus.PENDING);
        long running = items.countByJobAndStatus(job, BatchItemStatus.RUNNING);
        if (pending == 0 && running == 0 && job.getStatus() == BatchJobStatus.RUNNING) {
            job.complete();
        }
    }

    private BatchJob ownedJob(Long id, User user) {
        return jobs.findByIdAndOwner(id, user).orElseThrow(() -> new EntityNotFoundException("批量任务不存在"));
    }

    private BatchJobItem ownedItem(BatchJob job, Long itemId) {
        BatchJobItem item = items.findById(itemId).orElseThrow(() -> new EntityNotFoundException("任务项不存在"));
        if (!item.getJob().getId().equals(job.getId())) {
            throw new EntityNotFoundException("任务项不存在");
        }
        return item;
    }

    private BatchDtos.BatchJobResponse toJobResponse(BatchJob job) {
        return new BatchDtos.BatchJobResponse(
                job.getId(),
                job.getName(),
                job.getStatus().name(),
                job.getTotalCount(),
                job.getSuccessCount(),
                job.getFailedCount(),
                (int) items.countByJobAndStatus(job, BatchItemStatus.PENDING),
                (int) items.countByJobAndStatus(job, BatchItemStatus.RUNNING),
                job.getCreatedAt()
        );
    }

    private BatchDtos.BatchItemResponse toItemResponse(BatchJobItem item) {
        Optional<Problem> problem = item.getProblemId() == null ? Optional.empty() : problems.findById(item.getProblemId());
        return new BatchDtos.BatchItemResponse(
                item.getId(),
                item.getTitle(),
                item.getContent(),
                item.getStatus().name(),
                item.getSortOrder(),
                item.getProblemId(),
                problem.map(value -> value.getDifficulty().label()).orElse(null),
                problem.map(value -> value.getDifficulty().name()).orElse(null),
                problem.map(value -> value.getTags().stream().sorted().toList()).orElse(List.of()),
                item.getErrorMessage(),
                item.getCreatedAt()
        );
    }

    private String readUtf8(MultipartFile file) {
        try {
            return new String(file.getBytes(), StandardCharsets.UTF_8);
        } catch (IOException exception) {
            throw new IllegalArgumentException("文件读取失败：" + file.getOriginalFilename());
        }
    }

    private String titleFromFilename(String filename) {
        String clean = filename.replace('\\', '/');
        int slash = clean.lastIndexOf('/');
        if (slash >= 0) {
            clean = clean.substring(slash + 1);
        }
        String lower = clean.toLowerCase();
        if (lower.endsWith(".txt")) {
            clean = clean.substring(0, clean.length() - 4);
        } else if (lower.endsWith(".md")) {
            clean = clean.substring(0, clean.length() - 3);
        }
        return clean.isBlank() ? "未命名题目" : clean;
    }

    private boolean isTextProblemFile(String filename) {
        String lower = filename.toLowerCase();
        return lower.endsWith(".txt") || lower.endsWith(".md");
    }

    private String defaultName(String name) {
        return name == null || name.isBlank() ? "批量导入任务" : name.trim();
    }

    @PreDestroy
    void shutdown() {
        executor.shutdownNow();
    }

    private record WorkItem(Long itemId, Long jobId, Long ownerId, String title, String content) {
    }
}
