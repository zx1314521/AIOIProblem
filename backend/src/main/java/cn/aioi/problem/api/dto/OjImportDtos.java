package cn.aioi.problem.api.dto;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.time.Instant;
import java.util.List;

public final class OjImportDtos {
    private OjImportDtos() {
    }

    public record OjImportRequest(@NotEmpty @Size(max = 200) List<@Valid @NotNull OjImportItem> items) {
    }

    public record OjImportItem(
            String platform,
            String sourceId,
            String title,
            String statement,
            String url,
            boolean passed,
            Instant submittedAt
    ) {
    }

    public record OjImportResponse(List<OjImportItemResult> items) {
    }

    public record OjImportItemResult(
            String sourceId,
            String title,
            String status,
            Long problemId,
            String message
    ) {
    }

    public record OjImportHistoryJob(
            Long id,
            String name,
            String status,
            int totalCount,
            int successCount,
            int failedCount,
            int pendingCount,
            int runningCount,
            Instant createdAt,
            List<OjImportHistoryItem> items
    ) {
    }

    public record OjImportHistoryItem(
            Long id,
            String platform,
            String sourceId,
            String title,
            String status,
            Long problemId,
            String sourceUrl,
            String originalStatement,
            boolean passedRequested,
            String errorMessage,
            Instant createdAt,
            Instant startedAt,
            Instant finishedAt,
            String aiProvider,
            String aiModel,
            Long aiDurationMs
    ) {
    }
}
