package cn.aioi.problem.ai;

import cn.aioi.problem.service.TagCatalogService;
import org.springframework.http.MediaType;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;

import java.time.Duration;
import java.util.List;
import java.util.Map;

@Component
public class DeepSeekAiProvider {
    private final AiProperties properties;
    private final AiAssessmentParser parser;
    private final TagCatalogService tagCatalog;

    public DeepSeekAiProvider(AiProperties properties, AiAssessmentParser parser, TagCatalogService tagCatalog) {
        this.properties = properties;
        this.parser = parser;
        this.tagCatalog = tagCatalog;
    }

    public AiAssessment assess(ProblemInput input) {
        AiProperties.DeepSeek deepseek = properties.deepseek();
        if (deepseek == null) {
            throw new IllegalStateException("DeepSeek 配置缺失");
        }
        if (deepseek.apiKey() == null || deepseek.apiKey().isBlank()) {
            throw new IllegalStateException("DeepSeek API Key 未配置");
        }
        return call(input, deepseek.baseUrl(), deepseek.apiKey(), deepseek.model(), deepseek.timeoutSeconds());
    }

    public AiAssessment assess(ProblemInput input, AiRuntimeSettings settings) {
        if (settings.deepSeekApiKey() == null || settings.deepSeekApiKey().isBlank()) {
            throw new IllegalStateException("DeepSeek API Key 未配置");
        }
        return call(input, settings.deepSeekBaseUrl(), settings.deepSeekApiKey(), settings.deepSeekModel(), settings.deepSeekTimeoutSeconds());
    }

    private AiAssessment call(ProblemInput input, String baseUrl, String apiKey, String model, int timeoutSeconds) {
        SimpleClientHttpRequestFactory requestFactory = new SimpleClientHttpRequestFactory();
        requestFactory.setConnectTimeout(Duration.ofSeconds(Math.max(1, timeoutSeconds)));
        requestFactory.setReadTimeout(Duration.ofSeconds(Math.max(1, timeoutSeconds)));
        RestClient client = RestClient.builder()
                .baseUrl(baseUrl)
                .requestFactory(requestFactory)
                .build();
        Map<String, Object> body = Map.of(
                "model", model,
                "messages", List.of(
                        Map.of("role", "system", "content", systemPrompt()),
                        Map.of("role", "user", "content", input.title() + "\n\n" + input.text())
                ),
                "temperature", 0.2
        );
        String response = client.post()
                .contentType(MediaType.APPLICATION_JSON)
                .header("Authorization", "Bearer " + apiKey)
                .body(body)
                .retrieve()
                .body(String.class);
        return parser.parse(response == null ? "{}" : response);
    }

    private String systemPrompt() {
        return """
                你是信息学竞赛题目分析器。只输出 JSON，不要 Markdown。
                字段：difficulty, confidence, tags, hints, reasoningSummary。
                difficulty 必须是：入门、简单、CSPJ中等、CSPS提高、NOIP困难、地狱NOI。
                hints 是由浅入深的 3 条短提示。
                """ + "\n" + tagCatalog.promptText();
    }
}
