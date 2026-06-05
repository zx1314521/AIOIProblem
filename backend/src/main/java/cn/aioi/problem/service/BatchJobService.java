package cn.aioi.problem.service;

import cn.aioi.problem.ai.AiAssessment;
import cn.aioi.problem.ai.AiProvider;
import cn.aioi.problem.ai.AiRuntimeSettings;
import cn.aioi.problem.ai.AiTaskType;
import cn.aioi.problem.ai.ProblemInput;
import cn.aioi.problem.api.dto.BatchDtos;
import cn.aioi.problem.domain.BatchItemStatus;
import cn.aioi.problem.domain.BatchJob;
import cn.aioi.problem.domain.BatchJobItem;
import cn.aioi.problem.domain.BatchJobStatus;
import cn.aioi.problem.domain.PassedProblem;
import cn.aioi.problem.domain.Problem;
import cn.aioi.problem.domain.User;
import cn.aioi.problem.repository.BatchJobItemRepository;
import cn.aioi.problem.repository.BatchJobRepository;
import cn.aioi.problem.repository.PassedProblemRepository;
import cn.aioi.problem.repository.ProblemRepository;
import cn.aioi.problem.repository.UserRepository;
import jakarta.annotation.PostConstruct;
import jakarta.annotation.PreDestroy;
import jakarta.persistence.EntityNotFoundException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
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
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicBoolean;

@Service
public class BatchJobService {
    private static final Logger log = LoggerFactory.getLogger(BatchJobService.class);
    private final BatchJobRepository jobs;
    private final BatchJobItemRepository items;
    private final ProblemRepository problems;
    private final PassedProblemRepository passedProblems;
    private final UserRepository users;
    private final AiProvider aiProvider;
    private final AiSettingsService aiSettingsService;
    private final TagCatalogService tagCatalog;
    private final TransactionTemplate transactionTemplate;
    private final ExecutorService executor = Executors.newSingleThreadExecutor();
    private final AtomicBoolean workerRunning = new AtomicBoolean(false);

    public BatchJobService(BatchJobRepository jobs, BatchJobItemRepository items, ProblemRepository problems,
                           PassedProblemRepository passedProblems, UserRepository users,
                           AiProvider aiProvider, AiSettingsService aiSettingsService,
                           TagCatalogService tagCatalog,
                           PlatformTransactionManager transactionManager) {
        this.jobs = jobs;
        this.items = items;
        this.problems = problems;
        this.passedProblems = passedProblems;
        this.users = users;
        this.aiProvider = aiProvider;
        this.aiSettingsService = aiSettingsService;
        this.tagCatalog = tagCatalog;
        this.transactionTemplate = new TransactionTemplate(transactionManager);
    }

    @PostConstruct
    void resumePendingWorkOnStartup() {
        transactionTemplate.executeWithoutResult(status -> {
            List<BatchJobItem> interruptedItems = items.findByStatus(BatchItemStatus.RUNNING);
            interruptedItems.forEach(BatchJobItem::returnToPendingAfterInterruption);
            if (!interruptedItems.isEmpty()) {
                log.info("Recovered {} interrupted batch item(s)", interruptedItems.size());
            }
        });
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
            items.save(new BatchJobItem(
                    job,
                    ProblemTextNormalizer.normalizeNamesAndTrim(titleFromFilename(filename)),
                    ProblemTextNormalizer.normalizeNamesAndTrim(readUtf8(file)),
                    (int) items.countByJobAndStatus(job, BatchItemStatus.PENDING)
            ));
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
    public BatchDtos.BatchJobDetailResponse reanalyzeProblems(List<Long> problemIds, User user) {
        List<Long> ids = distinctIds(problemIds);
        if (ids.isEmpty()) {
            throw new IllegalArgumentException("请选择至少一个题目");
        }
        BatchJob job = jobs.save(new BatchJob("重新分析题目", user, ids.size()));
        int index = 0;
        for (Long problemId : ids) {
            Problem problem = problems.findById(problemId).orElseThrow(() -> new EntityNotFoundException("题目不存在"));
            items.save(BatchJobItem.reanalysis(
                    job,
                    ProblemTextNormalizer.normalizeNamesAndTrim(problem.getTitle()),
                    ProblemTextNormalizer.normalizeNamesAndTrim(problem.getDescription()),
                    index++,
                    problem.getId()
            ));
        }
        triggerWorkerAfterCommit();
        return detail(job.getId(), user);
    }

    @Transactional
    public BatchDtos.BatchItemResponse updateItem(Long jobId, Long itemId, BatchDtos.BatchItemUpdateRequest request, User user) {
        BatchJob job = ownedJob(jobId, user);
        BatchJobItem item = ownedItem(job, itemId);
        item.updatePending(
                ProblemTextNormalizer.normalizeNamesAndTrim(request.title()),
                ProblemTextNormalizer.normalizeNamesAndTrim(request.content())
        );
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
        } else {
            executor.submit(() -> {
                if (workerRunning.compareAndSet(false, true)) {
                    workerLoop();
                }
            });
        }
    }

    public void triggerWorkerAfterCommit() {
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
        } catch (RuntimeException exception) {
            log.error("Batch worker stopped unexpectedly", exception);
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
            Long targetProblemId = item.getReanalysisProblemId();
            Optional<Problem> targetProblem = targetProblemId == null ? Optional.empty() : problems.findById(targetProblemId);
            return new WorkItem(
                    item.getId(),
                    item.getJob().getId(),
                    item.getJob().getOwner().getId(),
                    ProblemTextNormalizer.normalizeNamesAndTrim(targetProblem.map(Problem::getTitle).orElse(item.getTitle())),
                    ProblemTextNormalizer.normalizeNamesAndTrim(targetProblem.map(Problem::getDescription).orElse(item.getContent())),
                    targetProblemId,
                    item.getExternalPlatform(),
                    item.getExternalSourceId(),
                    item.getSourceUrl(),
                    item.isMarkPassedAfterImport()
            );
        });
        if (workItem == null) {
            return;
        }
        AiRuntimeSettings runtime = aiSettingsService.runtimeSettings(AiTaskType.PROBLEM_ANALYSIS);
        long started = System.nanoTime();
        try {
            if (workItem.reanalysisProblemId() == null && completeExistingExternalImport(workItem)) {
                return;
            }
            String content = contentForAnalysis(workItem);
            AiAssessment assessment = aiProvider.assess(new ProblemInput(workItem.title(), content), AiTaskType.PROBLEM_ANALYSIS);
            long durationMs = elapsedMs(started);
            transactionTemplate.executeWithoutResult(status -> {
                BatchJobItem item = items.findById(workItem.itemId()).orElseThrow();
                User owner = users.getReferenceById(workItem.ownerId());
                Problem problem = targetProblem(workItem);
                boolean created = false;
                if (problem == null) {
                    problem = problems.save(new Problem(
                            ProblemTextNormalizer.normalizeNamesAndTrim(workItem.title()),
                            content,
                            assessment.difficulty(),
                            ProblemService.sanitizeAiTags(assessment.tags(), tagCatalog),
                            sourceLabel(workItem),
                            owner,
                            workItem.externalPlatform(),
                            workItem.externalSourceId(),
                            workItem.sourceUrl()
                    ));
                    created = true;
                } else if (workItem.reanalysisProblemId() != null) {
                    problem.update(
                            problem.getTitle(),
                            content,
                            assessment.difficulty(),
                            ProblemService.sanitizeAiTags(assessment.tags(), tagCatalog),
                            problem.getSource()
                    );
                }
                markPassedIfNeeded(problem, owner, workItem.markPassedAfterImport(), created);
                item.completeAnalysisMetadata(
                        providerLabel(runtime, assessment),
                        modelLabel(runtime, assessment),
                        assessment.confidence(),
                        assessment.reasoningSummary(),
                        encodeHints(assessment.hints()),
                        durationMs
                );
                item.succeed(problem.getId());
                item.getJob().incrementSuccess();
                completeJobIfDone(item.getJob());
            });
        } catch (RuntimeException exception) {
            long durationMs = elapsedMs(started);
            transactionTemplate.executeWithoutResult(status -> {
                BatchJobItem item = items.findById(workItem.itemId()).orElseThrow();
                item.fail(exception.getMessage(), runtime.providerLabel(), modelLabel(runtime), durationMs);
                item.getJob().incrementFailed();
                completeJobIfDone(item.getJob());
            });
        }
    }

    private boolean completeExistingExternalImport(WorkItem workItem) {
        if (workItem.externalPlatform() == null || workItem.externalSourceId() == null) {
            return false;
        }
        return Boolean.TRUE.equals(transactionTemplate.execute(status -> {
            Problem problem = existingExternalProblem(workItem);
            if (problem == null) {
                return false;
            }
            BatchJobItem item = items.findById(workItem.itemId()).orElseThrow();
            User owner = users.getReferenceById(workItem.ownerId());
            markPassedIfNeeded(problem, owner, workItem.markPassedAfterImport(), false);
            item.completeAnalysisMetadata(
                    "OJ import",
                    "existing problem",
                    1.0,
                    "同源题已存在，跳过 AI 分析。",
                    "[]",
                    0
            );
            item.succeed(problem.getId());
            item.getJob().incrementSuccess();
            completeJobIfDone(item.getJob());
            return true;
        }));
    }

    private void completeJobIfDone(BatchJob job) {
        long pending = items.countByJobAndStatus(job, BatchItemStatus.PENDING);
        long running = items.countByJobAndStatus(job, BatchItemStatus.RUNNING);
        if (pending == 0 && running == 0 && job.getStatus() == BatchJobStatus.RUNNING) {
            job.complete();
        }
    }

    private Problem existingExternalProblem(WorkItem workItem) {
        if (workItem.externalPlatform() == null || workItem.externalSourceId() == null) {
            return null;
        }
        return problems.findByExternalPlatformAndExternalSourceId(workItem.externalPlatform(), workItem.externalSourceId())
                .orElse(null);
    }

    private Problem targetProblem(WorkItem workItem) {
        if (workItem.reanalysisProblemId() != null) {
            return problems.findById(workItem.reanalysisProblemId()).orElseThrow(() -> new EntityNotFoundException("题目不存在"));
        }
        return existingExternalProblem(workItem);
    }

    private String contentForAnalysis(WorkItem workItem) {
        try {
            String polished = aiProvider.polishProblemStatement(new ProblemInput(workItem.title(), workItem.content()), AiTaskType.PROBLEM_ANALYSIS);
            return polished == null || polished.isBlank()
                    ? ProblemTextNormalizer.normalizeNamesAndTrim(workItem.content())
                    : ProblemTextNormalizer.normalizeNamesAndTrim(polished);
        } catch (RuntimeException exception) {
            return ProblemTextNormalizer.normalizeNamesAndTrim(workItem.content());
        }
    }

    private void markPassedIfNeeded(Problem problem, User owner, boolean markPassed, boolean created) {
        if (markPassed && (created || !passedProblems.existsByUserAndProblem(owner, problem))) {
            passedProblems.save(new PassedProblem(owner, problem));
        }
    }

    private String sourceLabel(WorkItem workItem) {
        if (workItem.externalPlatform() == null || workItem.externalSourceId() == null) {
            return "批量导入";
        }
        return platformLabel(workItem.externalPlatform()) + ": " + workItem.externalSourceId();
    }

    private String platformLabel(String platform) {
        return switch (platform) {
            case "CODEFORCES" -> "Codeforces";
            case "ATCODER" -> "AtCoder";
            case "LUOGU" -> "洛谷";
            case "NOWCODER" -> "牛客";
            default -> platform;
        };
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
        Long responseProblemId = item.getProblemId() == null ? item.getReanalysisProblemId() : item.getProblemId();
        Optional<Problem> problem = responseProblemId == null ? Optional.empty() : problems.findById(responseProblemId);
        return new BatchDtos.BatchItemResponse(
                item.getId(),
                item.getTitle(),
                item.getContent(),
                item.getStatus().name(),
                item.getSortOrder(),
                responseProblemId,
                problem.map(value -> value.getDifficulty().label()).orElse(null),
                problem.map(value -> value.getDifficulty().name()).orElse(null),
                problem.map(value -> value.getTags().stream().sorted().toList()).orElse(List.of()),
                item.getErrorMessage(),
                item.getCreatedAt(),
                item.getStartedAt(),
                item.getFinishedAt(),
                item.getAiProvider(),
                item.getAiModel(),
                item.getAiConfidence(),
                item.getAiReasoningSummary(),
                decodeHints(item.getAiHints()),
                item.getAiDurationMs()
        );
    }

    private String providerLabel(AiRuntimeSettings runtime, AiAssessment assessment) {
        if (usedRuleFallback(assessment)) {
            return "本地规则模型（" + runtime.providerLabel() + "失败后兜底）";
        }
        return runtime.providerLabel();
    }

    private String modelLabel(AiRuntimeSettings runtime, AiAssessment assessment) {
        if (usedRuleFallback(assessment)) {
            return "规则模型";
        }
        return modelLabel(runtime);
    }

    private String modelLabel(AiRuntimeSettings runtime) {
        return switch (runtime.provider()) {
            case "deepseek" -> runtime.deepSeekModel();
            case "codex" -> runtime.codexCommand();
            case "mock" -> "本地规则模型";
            default -> runtime.provider();
        };
    }

    private boolean usedRuleFallback(AiAssessment assessment) {
        return assessment.reasoningSummary().contains("已使用本地规则模型兜底");
    }

    private long elapsedMs(long started) {
        return java.time.Duration.ofNanos(System.nanoTime() - started).toMillis();
    }

    private String encodeHints(List<String> hints) {
        if (hints == null || hints.isEmpty()) {
            return "";
        }
        return hints.stream()
                .map(hint -> hint.replace("\r", " ").replace("\n", " ").trim())
                .filter(hint -> !hint.isBlank())
                .collect(java.util.stream.Collectors.joining("\n"));
    }

    private List<String> decodeHints(String hints) {
        if (hints == null || hints.isBlank()) {
            return List.of();
        }
        return hints.lines()
                .map(String::trim)
                .filter(line -> !line.isBlank())
                .toList();
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

    private static List<Long> distinctIds(List<Long> ids) {
        if (ids == null) {
            return List.of();
        }
        return ids.stream()
                .distinct()
                .toList();
    }

    @PreDestroy
    void shutdown() {
        executor.shutdownNow();
    }

    private record WorkItem(Long itemId, Long jobId, Long ownerId, String title, String content,
                            Long reanalysisProblemId, String externalPlatform, String externalSourceId, String sourceUrl,
                            boolean markPassedAfterImport) {
    }
}
