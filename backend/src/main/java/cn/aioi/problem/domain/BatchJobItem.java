package cn.aioi.problem.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.PrePersist;
import jakarta.persistence.Table;

import java.time.Instant;

@Entity
@Table(name = "batch_job_items")
public class BatchJobItem {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "job_id", nullable = false)
    private BatchJob job;

    @Column(nullable = false, length = 220)
    private String title;

    @Column(nullable = false, columnDefinition = "TEXT")
    private String content;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 24)
    private BatchItemStatus status;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 32)
    private BatchItemTaskType taskType = BatchItemTaskType.PROBLEM_ANALYSIS;

    @Column(nullable = false)
    private int sortOrder;

    private Long problemId;

    private Long reanalysisProblemId;

    @Column(columnDefinition = "TEXT")
    private String errorMessage;

    @Column(nullable = false)
    private Instant createdAt;

    private Instant startedAt;
    private Instant finishedAt;

    @Column(length = 80)
    private String aiProvider;

    @Column(length = 160)
    private String aiModel;

    private Double aiConfidence;

    @Column(columnDefinition = "TEXT")
    private String aiReasoningSummary;

    @Column(columnDefinition = "TEXT")
    private String aiHints;

    private Long aiDurationMs;

    @Column(length = 32)
    private String externalPlatform;

    @Column(length = 80)
    private String externalSourceId;

    @Column(length = 512)
    private String sourceUrl;

    @Column(nullable = false)
    private boolean markPassedAfterImport;

    protected BatchJobItem() {
    }

    public BatchJobItem(BatchJob job, String title, String content) {
        this(job, title, content, 0);
    }

    public BatchJobItem(BatchJob job, String title, String content, int sortOrder) {
        this.job = job;
        this.title = title;
        this.content = content;
        this.status = BatchItemStatus.PENDING;
        this.sortOrder = sortOrder;
    }

    public static BatchJobItem ojImport(BatchJob job, String title, String content, int sortOrder,
                                        String externalPlatform, String externalSourceId, String sourceUrl,
                                        boolean markPassedAfterImport) {
        BatchJobItem item = new BatchJobItem(job, title, content, sortOrder);
        item.externalPlatform = externalPlatform;
        item.externalSourceId = externalSourceId;
        item.sourceUrl = sourceUrl;
        item.markPassedAfterImport = markPassedAfterImport;
        return item;
    }

    public static BatchJobItem reanalysis(BatchJob job, String title, String content, int sortOrder, Long problemId) {
        BatchJobItem item = new BatchJobItem(job, title, content, sortOrder);
        item.reanalysisProblemId = problemId;
        return item;
    }

    public static BatchJobItem dataGeneration(BatchJob job, String title, String content, int sortOrder, Long problemId) {
        BatchJobItem item = new BatchJobItem(job, title, content, sortOrder);
        item.taskType = BatchItemTaskType.DATA_GENERATION;
        item.reanalysisProblemId = problemId;
        return item;
    }

    @PrePersist
    void prePersist() {
        if (createdAt == null) {
            createdAt = Instant.now();
        }
        if (taskType == null) {
            taskType = BatchItemTaskType.PROBLEM_ANALYSIS;
        }
    }

    public void start() {
        status = BatchItemStatus.RUNNING;
        startedAt = Instant.now();
    }

    public void returnToPendingAfterInterruption() {
        if (status == BatchItemStatus.RUNNING) {
            status = BatchItemStatus.PENDING;
            startedAt = null;
        }
    }

    public void succeed(Long problemId) {
        status = BatchItemStatus.SUCCEEDED;
        this.problemId = problemId;
        finishedAt = Instant.now();
    }

    public void completeAnalysisMetadata(String aiProvider, String aiModel, double aiConfidence,
                                         String aiReasoningSummary, String aiHints, long aiDurationMs) {
        this.aiProvider = aiProvider;
        this.aiModel = aiModel;
        this.aiConfidence = aiConfidence;
        this.aiReasoningSummary = aiReasoningSummary;
        this.aiHints = aiHints;
        this.aiDurationMs = aiDurationMs;
    }

    public void fail(String errorMessage) {
        status = BatchItemStatus.FAILED;
        this.errorMessage = errorMessage;
        finishedAt = Instant.now();
    }

    public void fail(String errorMessage, String aiProvider, String aiModel, long aiDurationMs) {
        this.aiProvider = aiProvider;
        this.aiModel = aiModel;
        this.aiDurationMs = aiDurationMs;
        fail(errorMessage);
    }

    public void updatePending(String title, String content) {
        ensurePending();
        this.title = title;
        this.content = content;
    }

    public void setSortOrder(int sortOrder) {
        ensurePending();
        this.sortOrder = sortOrder;
    }

    private void ensurePending() {
        if (status != BatchItemStatus.PENDING) {
            throw new IllegalStateException("只有等待中的任务可以修改");
        }
    }

    public Long getId() {
        return id;
    }

    public BatchJob getJob() {
        return job;
    }

    public String getTitle() {
        return title;
    }

    public String getContent() {
        return content;
    }

    public BatchItemStatus getStatus() {
        return status;
    }

    public BatchItemTaskType getTaskType() {
        return taskType;
    }

    public int getSortOrder() {
        return sortOrder;
    }

    public Long getProblemId() {
        return problemId;
    }

    public Long getReanalysisProblemId() {
        return reanalysisProblemId;
    }

    public String getErrorMessage() {
        return errorMessage;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }

    public Instant getStartedAt() {
        return startedAt;
    }

    public Instant getFinishedAt() {
        return finishedAt;
    }

    public String getAiProvider() {
        return aiProvider;
    }

    public String getAiModel() {
        return aiModel;
    }

    public Double getAiConfidence() {
        return aiConfidence;
    }

    public String getAiReasoningSummary() {
        return aiReasoningSummary;
    }

    public String getAiHints() {
        return aiHints;
    }

    public Long getAiDurationMs() {
        return aiDurationMs;
    }

    public String getExternalPlatform() {
        return externalPlatform;
    }

    public String getExternalSourceId() {
        return externalSourceId;
    }

    public String getSourceUrl() {
        return sourceUrl;
    }

    public boolean isMarkPassedAfterImport() {
        return markPassedAfterImport;
    }
}
