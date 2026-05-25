package cn.aioi.problem.api.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;

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
            String content,
            String status,
            int sortOrder,
            Long problemId,
            String difficulty,
            String difficultyCode,
            List<String> tags,
            String errorMessage,
            Instant createdAt,
            Instant startedAt,
            Instant finishedAt,
            String aiProvider,
            String aiModel,
            Double aiConfidence,
            String aiReasoningSummary,
            List<String> aiHints,
            Long aiDurationMs
    ) {
    }

    public record BatchJobDetailResponse(BatchJobResponse job, List<BatchItemResponse> items) {
    }

    public record BatchItemUpdateRequest(@NotBlank String title, @NotBlank String content) {
    }

    public record BatchItemReorderRequest(@NotEmpty List<Long> itemIds) {
    }
}
