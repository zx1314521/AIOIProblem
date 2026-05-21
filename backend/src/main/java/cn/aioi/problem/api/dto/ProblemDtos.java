package cn.aioi.problem.api.dto;

import cn.aioi.problem.domain.DifficultyLevel;
import cn.aioi.problem.domain.Problem;
import jakarta.validation.constraints.NotBlank;
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

    public record ProblemResponse(
            Long id,
            String title,
            String description,
            String difficulty,
            String difficultyCode,
            List<String> tags,
            String source,
            Instant createdAt,
            boolean passed
    ) {
        public static ProblemResponse from(Problem problem, boolean passed) {
            DifficultyLevel difficulty = problem.getDifficulty();
            return new ProblemResponse(
                    problem.getId(),
                    problem.getTitle(),
                    problem.getDescription(),
                    difficulty.label(),
                    difficulty.name(),
                    problem.getTags().stream().sorted().toList(),
                    problem.getSource(),
                    problem.getCreatedAt(),
                    passed
            );
        }
    }
}

