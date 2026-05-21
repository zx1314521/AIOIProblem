package cn.aioi.problem.api.dto;

import jakarta.validation.constraints.NotBlank;

import java.util.List;

public final class AnalysisDtos {
    private AnalysisDtos() {
    }

    public record TextAnalysisRequest(@NotBlank String text, String title) {
    }

    public record SimilarProblem(Long id, String title, String difficulty, List<String> tags, String reason) {
    }

    public record AnalysisResponse(
            String difficulty,
            String difficultyCode,
            double confidence,
            List<String> tags,
            List<String> hints,
            String reasoningSummary,
            List<SimilarProblem> similarProblems
    ) {
    }
}

