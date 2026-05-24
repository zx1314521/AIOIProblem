package cn.aioi.problem.ai;

import cn.aioi.problem.service.AiSettingsService;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

class AiProviderRouterTest {
    @Test
    void codexFailuresPropagateInsteadOfFallingBackToRules() {
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

        assertThatThrownBy(() -> router.assess(input))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("Codex CLI 调用失败");
        verifyNoInteractions(ruleBased);
    }
}
