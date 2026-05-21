package cn.aioi.problem.api.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.time.Instant;
import java.util.List;

public final class ProblemSetDtos {
    private ProblemSetDtos() {
    }

    public record ProblemSetRequest(@NotBlank @Size(max = 160) String name, String description) {
    }

    public record AddProblemRequest(@NotNull Long problemId) {
    }

    public record ProblemSetResponse(
            Long id,
            String name,
            String description,
            Instant createdAt,
            List<ProblemDtos.ProblemResponse> problems
    ) {
    }
}

