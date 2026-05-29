package cn.aioi.problem.service;

import cn.aioi.problem.api.dto.OjImportDtos;
import cn.aioi.problem.domain.BatchItemStatus;
import cn.aioi.problem.domain.BatchJob;
import cn.aioi.problem.domain.BatchJobItem;
import cn.aioi.problem.domain.PassedProblem;
import cn.aioi.problem.domain.Problem;
import cn.aioi.problem.domain.User;
import cn.aioi.problem.repository.BatchJobItemRepository;
import cn.aioi.problem.repository.BatchJobRepository;
import cn.aioi.problem.repository.PassedProblemRepository;
import cn.aioi.problem.repository.ProblemRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.TransactionTemplate;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import java.util.regex.Pattern;

@Service
public class OjImportService {
    private static final Pattern SOURCE_ID_PATTERN = Pattern.compile("[A-Z0-9_\\-]{3,80}");
    private static final Set<String> PLATFORMS = Set.of("CODEFORCES", "ATCODER", "LUOGU", "NOWCODER");
    private static final int MAX_TITLE_LENGTH = 220;
    private static final int MAX_URL_LENGTH = 512;
    private static final int MAX_STATEMENT_LENGTH = 60_000;

    private final BatchJobRepository jobs;
    private final BatchJobItemRepository items;
    private final ProblemRepository problems;
    private final PassedProblemRepository passedProblems;
    private final BatchJobService batchJobService;
    private final TransactionTemplate transactionTemplate;

    public OjImportService(BatchJobRepository jobs, BatchJobItemRepository items, ProblemRepository problems,
                           PassedProblemRepository passedProblems, BatchJobService batchJobService,
                           PlatformTransactionManager transactionManager) {
        this.jobs = jobs;
        this.items = items;
        this.problems = problems;
        this.passedProblems = passedProblems;
        this.batchJobService = batchJobService;
        this.transactionTemplate = new TransactionTemplate(transactionManager);
    }

    public OjImportDtos.OjImportResponse importItems(OjImportDtos.OjImportRequest request, User user) {
        ImportOutcome outcome = transactionTemplate.execute(status -> importItemsInTransaction(request, user));
        if (outcome == null) {
            return new OjImportDtos.OjImportResponse(List.of());
        }
        if (outcome.triggerWorker()) {
            batchJobService.triggerWorker();
        }
        return outcome.response();
    }

    public List<OjImportDtos.OjImportHistoryJob> history(User user) {
        return jobs.findByOwnerOrderByCreatedAtDesc(user).stream()
                .map(job -> {
                    List<OjImportDtos.OjImportHistoryItem> historyItems = items.findByJobOrderBySortOrderAscIdAsc(job).stream()
                            .filter(item -> item.getExternalPlatform() != null)
                            .map(this::historyItem)
                            .toList();
                    if (historyItems.isEmpty()) {
                        return null;
                    }
                    return new OjImportDtos.OjImportHistoryJob(
                            job.getId(),
                            job.getName(),
                            job.getStatus().name(),
                            job.getTotalCount(),
                            job.getSuccessCount(),
                            job.getFailedCount(),
                            (int) items.countByJobAndStatus(job, BatchItemStatus.PENDING),
                            (int) items.countByJobAndStatus(job, BatchItemStatus.RUNNING),
                            job.getCreatedAt(),
                            historyItems
                    );
                })
                .filter(java.util.Objects::nonNull)
                .toList();
    }

    private ImportOutcome importItemsInTransaction(OjImportDtos.OjImportRequest request, User user) {
        List<PendingImport> pending = new ArrayList<>();
        List<OjImportDtos.OjImportItemResult> results = new ArrayList<>();
        Set<String> queuedInRequest = new HashSet<>();
        for (OjImportDtos.OjImportItem item : request.items()) {
            NormalizedImport normalized = normalize(item);
            if (normalized.empty()) {
                results.add(result(item.sourceId(), item.title(), "SKIPPED_EMPTY", null, "题目标题或题面为空"));
                continue;
            }
            if (!normalized.validContentSize()) {
                results.add(result(normalized.sourceId(), normalized.title(), "SKIPPED_EMPTY", null, "题目标题或题面过长"));
                continue;
            }
            if (!normalized.validSource()) {
                results.add(result(item.sourceId(), item.title(), "SKIPPED_INVALID_SOURCE_ID", null, "来源题号无效"));
                continue;
            }
            if (!normalized.reachable()) {
                results.add(result(normalized.sourceId(), normalized.title(), "SKIPPED_INACCESSIBLE", null, "题面地址不可访问"));
                continue;
            }
            Problem existing = problems.findByExternalPlatformAndExternalSourceId(normalized.platform(), normalized.sourceId()).orElse(null);
            if (existing != null) {
                if (normalized.passed() && !passedProblems.existsByUserAndProblem(user, existing)) {
                    passedProblems.save(new PassedProblem(user, existing));
                    results.add(result(normalized.sourceId(), normalized.title(), "EXISTS_MARKED_PASSED", existing.getId(), "已存在并标记通过"));
                } else {
                    results.add(result(normalized.sourceId(), normalized.title(), "EXISTS_UNCHANGED", existing.getId(), "已存在"));
                }
                continue;
            }
            String importKey = normalized.platform() + ":" + normalized.sourceId();
            if (queuedInRequest.contains(importKey)) {
                results.add(result(normalized.sourceId(), normalized.title(), "EXISTS_UNCHANGED", null, "本次请求中已加入队列"));
                continue;
            }
            if (items.existsByExternalPlatformAndExternalSourceIdAndStatusIn(
                    normalized.platform(), normalized.sourceId(), List.of(BatchItemStatus.PENDING, BatchItemStatus.RUNNING))) {
                results.add(result(normalized.sourceId(), normalized.title(), "EXISTS_UNCHANGED", null, "已在分析队列中"));
                continue;
            }
            pending.add(new PendingImport(normalized));
            queuedInRequest.add(importKey);
            results.add(result(normalized.sourceId(), normalized.title(), "QUEUED", null, "已加入 AI 分析队列"));
        }

        if (!pending.isEmpty()) {
            BatchJob job = jobs.save(new BatchJob("OJ 导入", user, pending.size()));
            for (int index = 0; index < pending.size(); index++) {
                NormalizedImport item = pending.get(index).item();
                items.save(BatchJobItem.ojImport(
                        job,
                        item.title(),
                        item.statement(),
                        index,
                        item.platform(),
                        item.sourceId(),
                        item.url(),
                        item.passed()
                ));
            }
        }
        return new ImportOutcome(new OjImportDtos.OjImportResponse(results), !pending.isEmpty());
    }

    private NormalizedImport normalize(OjImportDtos.OjImportItem item) {
        String platform = clean(item.platform()).toUpperCase(Locale.ROOT);
        String sourceId = clean(item.sourceId()).toUpperCase(Locale.ROOT);
        String title = ProblemTextNormalizer.normalizeNamesAndTrim(item.title());
        String statement = ProblemTextNormalizer.normalizeNamesAndTrim(item.statement());
        String url = clean(item.url());
        boolean empty = title.isBlank() || statement.isBlank();
        boolean validContentSize = title.length() <= MAX_TITLE_LENGTH && statement.length() <= MAX_STATEMENT_LENGTH;
        boolean validSource = PLATFORMS.contains(platform) && SOURCE_ID_PATTERN.matcher(sourceId).matches();
        boolean reachable = url.length() <= MAX_URL_LENGTH && (url.startsWith("http://") || url.startsWith("https://"));
        return new NormalizedImport(platform, sourceId, title, statement, url, item.passed(), empty, validContentSize,
                validSource, reachable);
    }

    private String clean(String value) {
        return value == null ? "" : value.trim();
    }

    private OjImportDtos.OjImportItemResult result(String sourceId, String title, String status, Long problemId, String message) {
        return new OjImportDtos.OjImportItemResult(sourceId, title, status, problemId, message);
    }

    private OjImportDtos.OjImportHistoryItem historyItem(BatchJobItem item) {
        return new OjImportDtos.OjImportHistoryItem(
                item.getId(),
                item.getExternalPlatform(),
                item.getExternalSourceId(),
                item.getTitle(),
                item.getStatus().name(),
                item.getProblemId(),
                item.getSourceUrl(),
                item.getContent(),
                item.isMarkPassedAfterImport(),
                item.getErrorMessage(),
                item.getCreatedAt(),
                item.getStartedAt(),
                item.getFinishedAt(),
                item.getAiProvider(),
                item.getAiModel(),
                item.getAiDurationMs()
        );
    }

    private record NormalizedImport(String platform, String sourceId, String title, String statement, String url,
                                    boolean passed, boolean empty, boolean validContentSize, boolean validSource,
                                    boolean reachable) {
    }

    private record PendingImport(NormalizedImport item) {
    }

    private record ImportOutcome(OjImportDtos.OjImportResponse response, boolean triggerWorker) {
    }
}
