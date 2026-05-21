package cn.aioi.problem.api.dto;

import java.util.List;

public final class RecommendationDtos {
    private RecommendationDtos() {
    }

    public record RecommendationResponse(List<String> weakTags, List<RecommendationItem> items) {
    }

    public record RecommendationItem(
            ProblemDtos.ProblemResponse problem,
            String reason,
            int practiceOrder
    ) {
    }
}

