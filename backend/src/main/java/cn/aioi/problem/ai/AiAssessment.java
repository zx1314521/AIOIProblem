package cn.aioi.problem.ai;

import cn.aioi.problem.domain.DifficultyLevel;

import java.util.List;

public record AiAssessment(
        DifficultyLevel difficulty,
        double confidence,
        List<String> tags,
        List<String> hints,
        String reasoningSummary
) {
    public AiAssessment {
        confidence = Math.max(0.0, Math.min(1.0, confidence));
        tags = tags == null ? List.of() : tags.stream().filter(tag -> tag != null && !tag.isBlank()).distinct().limit(12).toList();
        hints = hints == null ? List.of() : hints.stream().filter(hint -> hint != null && !hint.isBlank()).limit(5).toList();
        reasoningSummary = reasoningSummary == null || reasoningSummary.isBlank() ? "基于题面关键词、数据范围与常见算法标签综合判断。" : reasoningSummary;
    }
}

