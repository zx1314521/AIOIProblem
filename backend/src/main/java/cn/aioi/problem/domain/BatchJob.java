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
@Table(name = "batch_jobs")
public class BatchJob {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, length = 180)
    private String name;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 24)
    private BatchJobStatus status;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "owner_id", nullable = false)
    private User owner;

    @Column(nullable = false)
    private int totalCount;

    @Column(nullable = false)
    private int successCount;

    @Column(nullable = false)
    private int failedCount;

    @Column(nullable = false)
    private Instant createdAt;

    private Instant updatedAt;

    protected BatchJob() {
    }

    public BatchJob(String name, User owner, int totalCount) {
        this.name = name;
        this.owner = owner;
        this.totalCount = totalCount;
        this.status = BatchJobStatus.RUNNING;
        this.successCount = 0;
        this.failedCount = 0;
    }

    @PrePersist
    void prePersist() {
        if (createdAt == null) {
            createdAt = Instant.now();
        }
        updatedAt = Instant.now();
    }

    public void pause() {
        if (status == BatchJobStatus.RUNNING) {
            status = BatchJobStatus.PAUSED;
            updatedAt = Instant.now();
        }
    }

    public void resume() {
        if (status == BatchJobStatus.PAUSED) {
            status = BatchJobStatus.RUNNING;
            updatedAt = Instant.now();
        }
    }

    public void complete() {
        status = BatchJobStatus.COMPLETED;
        updatedAt = Instant.now();
    }

    public void incrementSuccess() {
        successCount += 1;
        updatedAt = Instant.now();
    }

    public void incrementFailed() {
        failedCount += 1;
        updatedAt = Instant.now();
    }

    public Long getId() {
        return id;
    }

    public String getName() {
        return name;
    }

    public BatchJobStatus getStatus() {
        return status;
    }

    public User getOwner() {
        return owner;
    }

    public int getTotalCount() {
        return totalCount;
    }

    public int getSuccessCount() {
        return successCount;
    }

    public int getFailedCount() {
        return failedCount;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }
}
