package cn.aioi.problem.service;

import cn.aioi.problem.ai.AiAssessment;
import cn.aioi.problem.ai.AiProvider;
import cn.aioi.problem.ai.AiTaskType;
import cn.aioi.problem.ai.ProblemInput;
import cn.aioi.problem.domain.DifficultyLevel;
import cn.aioi.problem.repository.ProblemRepository;
import org.junit.jupiter.api.Test;
import org.mockito.InOrder;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.inOrder;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class AnalysisServiceTest {
    @Test
    void polishesStatementBeforeAnalyzingText() {
        AiProvider aiProvider = mock(AiProvider.class);
        ProblemRepository problems = mock(ProblemRepository.class);
        AnalysisService service = new AnalysisService(aiProvider, problems);
        ProblemInput raw = new ProblemInput("CF", "Raw English statement");
        ProblemInput polished = new ProblemInput("CF", "整理后的中文题面");

        when(aiProvider.polishProblemStatement(raw, AiTaskType.PROBLEM_ANALYSIS)).thenReturn(polished.text());
        when(aiProvider.assess(polished, AiTaskType.PROBLEM_ANALYSIS)).thenReturn(new AiAssessment(
                DifficultyLevel.EASY,
                0.8,
                List.of("没有标签"),
                List.of("先读题", "找模型", "估复杂度"),
                "基于整理后的题面分析。"
        ));
        when(problems.findAllWithTags()).thenReturn(List.of());

        var response = service.analyzeText("CF", "Raw English statement");

        assertThat(response.reasoningSummary()).isEqualTo("基于整理后的题面分析。");
        InOrder order = inOrder(aiProvider);
        order.verify(aiProvider).polishProblemStatement(raw, AiTaskType.PROBLEM_ANALYSIS);
        order.verify(aiProvider).assess(polished, AiTaskType.PROBLEM_ANALYSIS);
    }

    @Test
    void fallsBackToRawStatementWhenPolishingReturnsBlank() {
        AiProvider aiProvider = mock(AiProvider.class);
        ProblemRepository problems = mock(ProblemRepository.class);
        AnalysisService service = new AnalysisService(aiProvider, problems);
        ProblemInput raw = new ProblemInput("CF", "Raw statement");

        when(aiProvider.polishProblemStatement(raw, AiTaskType.PROBLEM_ANALYSIS)).thenReturn(" ");
        when(aiProvider.assess(raw, AiTaskType.PROBLEM_ANALYSIS)).thenReturn(new AiAssessment(
                DifficultyLevel.EASY,
                0.8,
                List.of("没有标签"),
                List.of("先读题", "找模型", "估复杂度"),
                "使用原始题面分析。"
        ));
        when(problems.findAllWithTags()).thenReturn(List.of());

        var response = service.analyzeText("CF", "Raw statement");

        assertThat(response.reasoningSummary()).isEqualTo("使用原始题面分析。");
    }
    @Test
    void normalizesWowoNameInTitleAndStatementBeforeAnalysis() {
        AiProvider aiProvider = mock(AiProvider.class);
        ProblemRepository problems = mock(ProblemRepository.class);
        AnalysisService service = new AnalysisService(aiProvider, problems);
        ProblemInput raw = new ProblemInput("BOB 的题", "Raw statement with BOB");
        ProblemInput polished = new ProblemInput("BOB 的题", "整理后的 BOB 题面");

        when(aiProvider.polishProblemStatement(raw, AiTaskType.PROBLEM_ANALYSIS)).thenReturn("整理后的 \u8717\u8717 题面");
        when(aiProvider.assess(polished, AiTaskType.PROBLEM_ANALYSIS)).thenReturn(new AiAssessment(
                DifficultyLevel.EASY,
                0.8,
                List.of("没有标签"),
                List.of("先读题", "找模型", "估复杂度"),
                "已规范化名称。"
        ));
        when(problems.findAllWithTags()).thenReturn(List.of());

        var response = service.analyzeText("\u8717\u8717 的题", "Raw statement with \u8717\u8717");

        assertThat(response.reasoningSummary()).isEqualTo("已规范化名称。");
    }
}
