package cn.aioi.problem.api.dto;

import java.time.Instant;
import java.util.List;

public final class BatchDtos {
    private BatchDtos() {
    }

    public record BatchJobResponse(
            Long id,
            String name,
            String status,
            int totalCount,
            int successCount,
            int failedCount,
            int pendingCount,
            int runningCount,
            Instant createdAt
    ) {
    }

    public record BatchItemResponse(
            Long id,
            String title,
            String status,
            Long problemId,
            String errorMessage,
            Instant createdAt
    ) {
    }

    public record BatchJobDetailResponse(BatchJobResponse job, List<BatchItemResponse> items) {
    }
}

