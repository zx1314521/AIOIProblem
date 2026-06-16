package cn.aioi.problem.ai;

import cn.aioi.problem.service.TagCatalogService;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
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
    private final ObjectMapper objectMapper;

    public DeepSeekAiProvider(AiProperties properties, AiAssessmentParser parser, TagCatalogService tagCatalog, ObjectMapper objectMapper) {
        this.properties = properties;
        this.parser = parser;
        this.tagCatalog = tagCatalog;
        this.objectMapper = objectMapper;
    }

    public AiAssessment assess(ProblemInput input) {
        AiProperties.DeepSeek deepseek = properties.deepseek();
        if (deepseek == null) {
            throw new IllegalStateException("DeepSeek settings are missing");
        }
        if (deepseek.apiKey() == null || deepseek.apiKey().isBlank()) {
            throw new IllegalStateException("DeepSeek API key is not configured");
        }
        return call(input, deepseek.baseUrl(), deepseek.apiKey(), deepseek.model(), deepseek.timeoutSeconds());
    }

    public AiAssessment assess(ProblemInput input, AiRuntimeSettings settings) {
        if (settings.deepSeekApiKey() == null || settings.deepSeekApiKey().isBlank()) {
            throw new IllegalStateException("DeepSeek API key is not configured");
        }
        return call(input, settings.deepSeekBaseUrl(), settings.deepSeekApiKey(), settings.deepSeekModel(), settings.deepSeekTimeoutSeconds());
    }

    public String polishProblemStatement(ProblemInput input, AiRuntimeSettings settings) {
        if (settings.deepSeekApiKey() == null || settings.deepSeekApiKey().isBlank()) {
            throw new IllegalStateException("DeepSeek API key is not configured");
        }
        String response = callText(
                settings.deepSeekBaseUrl(),
                settings.deepSeekApiKey(),
                settings.deepSeekModel(),
                settings.deepSeekTimeoutSeconds(),
                polishSystemPrompt(),
                input.title() + "\n\n" + input.text()
        );
        return extractMessageContent(response).trim();
    }

    public String generateTestData(ProblemInput input, AiRuntimeSettings settings) {
        if (settings.deepSeekApiKey() == null || settings.deepSeekApiKey().isBlank()) {
            throw new IllegalStateException("DeepSeek API key is not configured");
        }
        String response = callText(
                settings.deepSeekBaseUrl(),
                settings.deepSeekApiKey(),
                settings.deepSeekModel(),
                settings.deepSeekTimeoutSeconds(),
                testDataSystemPrompt(),
                input.title() + "\n\n" + input.text()
        );
        return extractMessageContent(response).trim();
    }

    private AiAssessment call(ProblemInput input, String baseUrl, String apiKey, String model, int timeoutSeconds) {
        String response = callText(baseUrl, apiKey, model, timeoutSeconds, systemPrompt(), input.title() + "\n\n" + input.text());
        return parser.parse(response == null ? "{}" : response);
    }

    private String callText(String baseUrl, String apiKey, String model, int timeoutSeconds, String systemPrompt, String userPrompt) {
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
                        Map.of("role", "system", "content", systemPrompt),
                        Map.of("role", "user", "content", userPrompt)
                ),
                "temperature", 0.2
        );
        String response = client.post()
                .contentType(MediaType.APPLICATION_JSON)
                .header("Authorization", "Bearer " + apiKey)
                .body(body)
                .retrieve()
                .body(String.class);
        return response == null ? "" : response;
    }

    private String systemPrompt() {
        return """
                You are a competitive programming problem analyzer. Output JSON only, without Markdown.
                Required fields: difficulty, confidence, tags, hints, reasoningSummary.
                difficulty must be one of: ENTRY, EASY, CSPJ_MEDIUM, CSPS_ADVANCED, NOIP_HARD, NOI_HELL.
                hints should contain three short hints ordered from shallow to deep.
                Prefer tags from this catalog:
                """ + "\n" + tagCatalog.promptText();
    }

    private String polishSystemPrompt() {
        return """
                You translate and clean competitive programming problem statements.
                Output only the polished Chinese statement body, without explanations, Markdown code fences, or JSON.
                Preserve meaning, input/output format, samples, constraints, math symbols, and variable names.
                Remove web navigation, buttons, ads, duplicated blank lines, and irrelevant page status text.
                If the original text is already Chinese, only improve formatting and remove noise.
                """;
    }

    private String testDataSystemPrompt() {
        return """
                You generate competitive-programming test data for an existing problem.
                Infer the input format, output format, constraints, time/memory intent, and algorithm type from the statement.
                Output JSON only, without Markdown or explanations.
                Required JSON shape:
                {"stdCpp":"","configYaml":"","notes":"","cases":[{"index":1,"input":"","output":""}]}
                Requirements:
                - Produce a correct C++17 reference solution as stdCpp.
                - Produce exactly 25 paired test cases with indexes 1 through 25 exactly once.
                - Cases 1-2 should be samples when present, otherwise minimal valid cases.
                - Cases 3-8 cover small scale and boundary properties.
                - Cases 9-11 are hack cases for common wrong solutions.
                - Cases 12-20 cover medium/large stress.
                - Cases 21-25 are mixed random regression cases.
                - configYaml should describe type/time/memory and all cases.
                """;
    }

    private String extractMessageContent(String response) {
        try {
            JsonNode root = objectMapper.readTree(response == null ? "{}" : response);
            JsonNode content = root.path("choices").path(0).path("message").path("content");
            return content.isTextual() ? content.asText() : response;
        } catch (Exception ignored) {
            return response == null ? "" : response;
        }
    }
}
