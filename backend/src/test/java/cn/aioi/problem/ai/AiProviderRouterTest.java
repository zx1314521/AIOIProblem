package cn.aioi.problem.ai;

import cn.aioi.problem.service.AiSettingsService;
import org.junit.jupiter.api.Test;

import cn.aioi.problem.domain.DifficultyLevel;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class AiProviderRouterTest {
    @Test
    void codexFailuresFallBackToRules() {
        AiSettingsService settingsService = mock(AiSettingsService.class);
        RuleBasedAiProvider ruleBased = mock(RuleBasedAiProvider.class);
        DeepSeekAiProvider deepSeek = mock(DeepSeekAiProvider.class);
        CodexCliAiProvider codexCli = mock(CodexCliAiProvider.class);
        AiProviderRouter router = new AiProviderRouter(settingsService, ruleBased, deepSeek, codexCli);
        ProblemInput input = new ProblemInput("测试题", "n <= 10");

        when(settingsService.runtimeSettings(AiTaskType.PROBLEM_ANALYSIS)).thenReturn(new AiRuntimeSettings(
                "codex",
                "",
                "https://api.deepseek.com/chat/completions",
                "deepseek-chat",
                45,
                "codex",
                "gpt-5.5",
                60
        ));
        when(codexCli.assess(any(ProblemInput.class), any(AiRuntimeSettings.class)))
                .thenThrow(new IllegalStateException("Codex CLI 调用失败"));
        when(ruleBased.assess(input)).thenReturn(new AiAssessment(
                DifficultyLevel.EASY,
                0.7,
                java.util.List.of("模拟"),
                java.util.List.of("先模拟"),
                "规则模型初判。"
        ));

        AiAssessment assessment = router.assess(input);

        assertThat(assessment.difficulty()).isEqualTo(DifficultyLevel.EASY);
        assertThat(assessment.confidence()).isEqualTo(0.62);
        assertThat(assessment.reasoningSummary()).contains("Codex CLI 调用失败").contains("本地规则模型兜底");
    }

    @Test
    void routesByTaskProvider() {
        AiSettingsService settingsService = mock(AiSettingsService.class);
        RuleBasedAiProvider ruleBased = mock(RuleBasedAiProvider.class);
        DeepSeekAiProvider deepSeek = mock(DeepSeekAiProvider.class);
        CodexCliAiProvider codexCli = mock(CodexCliAiProvider.class);
        AiProviderRouter router = new AiProviderRouter(settingsService, ruleBased, deepSeek, codexCli);
        ProblemInput input = new ProblemInput("推荐", "补弱项");
        AiRuntimeSettings recommendationSettings = new AiRuntimeSettings(
                "mock",
                "",
                "https://api.deepseek.com/chat/completions",
                "deepseek-chat",
                45,
                "codex",
                "gpt-5.5",
                180
        );
        when(settingsService.runtimeSettings(AiTaskType.RECOMMENDATION)).thenReturn(recommendationSettings);
        when(ruleBased.assess(input)).thenReturn(new AiAssessment(
                DifficultyLevel.EASY,
                0.7,
                java.util.List.of("模拟"),
                java.util.List.of("先模拟"),
                "规则模型初判。"
        ));

        AiAssessment assessment = router.assess(input, AiTaskType.RECOMMENDATION);

        assertThat(assessment.reasoningSummary()).isEqualTo("规则模型初判。");
    }

    @Test
    void routesProblemStatementPolishingThroughConfiguredProvider() {
        AiSettingsService settingsService = mock(AiSettingsService.class);
        RuleBasedAiProvider ruleBased = mock(RuleBasedAiProvider.class);
        DeepSeekAiProvider deepSeek = mock(DeepSeekAiProvider.class);
        CodexCliAiProvider codexCli = mock(CodexCliAiProvider.class);
        AiProviderRouter router = new AiProviderRouter(settingsService, ruleBased, deepSeek, codexCli);
        ProblemInput input = new ProblemInput("OJ", "raw statement");
        AiRuntimeSettings settings = new AiRuntimeSettings(
                "codex",
                "",
                "https://api.deepseek.com/chat/completions",
                "deepseek-chat",
                45,
                "codex",
                "gpt-5.5",
                180
        );
        when(settingsService.runtimeSettings(AiTaskType.PROBLEM_ANALYSIS)).thenReturn(settings);
        when(codexCli.polishProblemStatement(input, settings)).thenReturn("整理后的中文题面");

        String polished = router.polishProblemStatement(input, AiTaskType.PROBLEM_ANALYSIS);

        assertThat(polished).isEqualTo("整理后的中文题面");
    }

    @Test
    void problemStatementPolishingFallsBackToRuleBasedCleanup() {
        AiSettingsService settingsService = mock(AiSettingsService.class);
        RuleBasedAiProvider ruleBased = mock(RuleBasedAiProvider.class);
        DeepSeekAiProvider deepSeek = mock(DeepSeekAiProvider.class);
        CodexCliAiProvider codexCli = mock(CodexCliAiProvider.class);
        AiProviderRouter router = new AiProviderRouter(settingsService, ruleBased, deepSeek, codexCli);
        ProblemInput input = new ProblemInput("OJ", "raw statement");
        AiRuntimeSettings settings = new AiRuntimeSettings(
                "deepseek",
                "key",
                "https://api.deepseek.com/chat/completions",
                "deepseek-chat",
                45,
                "codex",
                "gpt-5.5",
                180
        );
        when(settingsService.runtimeSettings(AiTaskType.PROBLEM_ANALYSIS)).thenReturn(settings);
        when(deepSeek.polishProblemStatement(input, settings)).thenThrow(new IllegalStateException("failed"));
        when(ruleBased.polishProblemStatement(input)).thenReturn("fallback cleanup");

        String polished = router.polishProblemStatement(input, AiTaskType.PROBLEM_ANALYSIS);

        assertThat(polished).isEqualTo("fallback cleanup");
    }
}
