package cn.aioi.problem.api.dto;

import cn.aioi.problem.domain.DifficultyLevel;
import cn.aioi.problem.domain.Problem;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.time.Instant;
import java.util.List;
import java.util.Set;

public final class ProblemDtos {
    private ProblemDtos() {
    }

    public record ProblemRequest(
            @NotBlank @Size(max = 200) String title,
            @NotBlank String description,
            @NotNull String difficulty,
            Set<String> tags,
            String source
    ) {
    }

    public record BulkProblemRequest(@NotEmpty List<@NotNull Long> problemIds) {
    }

    public record DuplicateHint(
            Long id,
            String title,
            String difficulty,
            String difficultyCode,
            List<String> tags,
            String externalPlatform,
            String externalSourceId,
            String sourceUrl,
            int score,
            String reason
    ) {
        public static DuplicateHint from(Problem problem, int score, String reason) {
            DifficultyLevel difficulty = problem.getDifficulty();
            return new DuplicateHint(
                    problem.getId(),
                    problem.getTitle(),
                    difficulty.label(),
                    difficulty.name(),
                    problem.getTags().stream().sorted().toList(),
                    problem.getExternalPlatform(),
                    problem.getExternalSourceId(),
                    problem.getSourceUrl(),
                    score,
                    reason
            );
        }
    }

    public record ProblemResponse(
            Long id,
            String title,
            String description,
            String difficulty,
            String difficultyCode,
            List<String> tags,
            String source,
            String externalPlatform,
            String externalSourceId,
            String sourceUrl,
            Instant createdAt,
            boolean passed,
            String dataStatus
    ) {
        public static ProblemResponse from(Problem problem, boolean passed) {
            return from(problem, passed, "NONE");
        }

        public static ProblemResponse from(Problem problem, boolean passed, String dataStatus) {
            DifficultyLevel difficulty = problem.getDifficulty();
            return new ProblemResponse(
                    problem.getId(),
                    problem.getTitle(),
                    problem.getDescription(),
                    difficulty.label(),
                    difficulty.name(),
                    problem.getTags().stream().sorted().toList(),
                    problem.getSource(),
                    problem.getExternalPlatform(),
                    problem.getExternalSourceId(),
                    problem.getSourceUrl(),
                    problem.getCreatedAt(),
                    passed,
                    dataStatus == null ? "NONE" : dataStatus
            );
        }
    }
}
