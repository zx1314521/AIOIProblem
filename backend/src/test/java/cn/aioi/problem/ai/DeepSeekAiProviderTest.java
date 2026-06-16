package cn.aioi.problem.ai;

import org.junit.jupiter.api.Test;

import cn.aioi.problem.service.TagCatalogService;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.sun.net.httpserver.HttpServer;

import java.net.InetSocketAddress;
import java.nio.charset.StandardCharsets;

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

    @Test
    void readsApplicationJsonChatCompletionResponseAsText() throws Exception {
        HttpServer server = HttpServer.create(new InetSocketAddress("127.0.0.1", 0), 0);
        server.createContext("/chat/completions", exchange -> {
            byte[] response = """
                    {"choices":[{"message":{"content":"{\\"cases\\":[]}"}}]}
                    """.getBytes(StandardCharsets.UTF_8);
            exchange.getResponseHeaders().add("Content-Type", "application/json");
            exchange.sendResponseHeaders(200, response.length);
            exchange.getResponseBody().write(response);
            exchange.close();
        });
        server.start();
        try {
            String baseUrl = "http://127.0.0.1:" + server.getAddress().getPort();
            TagCatalogService tagCatalog = new TagCatalogService();
            ObjectMapper objectMapper = new ObjectMapper();
            DeepSeekAiProvider provider = new DeepSeekAiProvider(
                    new AiProperties("deepseek", null, null),
                    new AiAssessmentParser(objectMapper, tagCatalog),
                    tagCatalog,
                    objectMapper
            );
            AiRuntimeSettings settings = new AiRuntimeSettings(
                    "deepseek",
                    "test-key",
                    baseUrl,
                    "deepseek-v4-flash",
                    5,
                    "codex",
                    "gpt-5.5",
                    180
            );

            String generated = provider.generateTestData(new ProblemInput("A+B", "read two integers"), settings);

            assertThat(generated).isEqualTo("{\"cases\":[]}");
        } finally {
            server.stop(0);
        }
    }
}
