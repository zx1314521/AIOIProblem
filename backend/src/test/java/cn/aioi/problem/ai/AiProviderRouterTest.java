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

        when(settingsService.runtimeSettings()).thenReturn(new AiRuntimeSettings(
                "codex",
                "",
                "https://api.deepseek.com/chat/completions",
                "deepseek-chat",
                45,
                "codex",
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
}
