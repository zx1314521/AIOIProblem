package cn.aioi.problem.ai;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class DeepSeekAiProviderTest {
    @Test
    void normalizesBaseUrlToChatCompletionsEndpoint() {
        assertThat(DeepSeekAiProvider.chatCompletionsUrl("https://api.deepseek.com"))
                .isEqualTo("https://api.deepseek.com/chat/completions");
        assertThat(DeepSeekAiProvider.chatCompletionsUrl("https://api.deepseek.com/"))
                .isEqualTo("https://api.deepseek.com/chat/completions");
        assertThat(DeepSeekAiProvider.chatCompletionsUrl("https://api.deepseek.com/chat/completions"))
                .isEqualTo("https://api.deepseek.com/chat/completions");
        assertThat(DeepSeekAiProvider.chatCompletionsUrl("https://api.deepseek.com/v1"))
                .isEqualTo("https://api.deepseek.com/v1/chat/completions");
    }
}
