package cn.aioi.problem.ai;

import cn.aioi.problem.domain.DifficultyLevel;
import cn.aioi.problem.service.TagCatalogService;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;

@Component
public class AiAssessmentParser {
    private final ObjectMapper objectMapper;
    private final TagCatalogService tagCatalog;

    public AiAssessmentParser(ObjectMapper objectMapper, TagCatalogService tagCatalog) {
        this.objectMapper = objectMapper;
        this.tagCatalog = tagCatalog;
    }

    public AiAssessment parse(String raw) {
        try {
            String json = extractJson(raw);
            JsonNode node = objectMapper.readTree(json);
            JsonNode contentNode = node.at("/choices/0/message/content");
            if (!contentNode.isMissingNode() && contentNode.isTextual()) {
                node = objectMapper.readTree(extractJson(contentNode.asText()));
            }
            DifficultyLevel difficulty = DifficultyLevel.fromLabelOrName(node.path("difficulty").asText());
            double confidence = node.path("confidence").asDouble(0.65);
            List<String> tags = tagCatalog.normalizeAiTags(readArray(node.path("tags")));
            List<String> hints = readArray(node.path("hints"));
            String summary = node.path("reasoningSummary").asText("基于题面综合判断。");
            return new AiAssessment(difficulty, confidence, tags, hints, summary);
        } catch (Exception exception) {
            return new AiAssessment(DifficultyLevel.EASY, 0.35, List.of("模拟"), List.of("先尝试直接模拟题意。"), "AI 输出解析失败，已使用保守默认判断。");
        }
    }

    private List<String> readArray(JsonNode node) {
        if (!node.isArray()) {
            return List.of();
        }
        List<String> values = new ArrayList<>();
        node.forEach(item -> values.add(item.asText()));
        return values;
    }

    private String extractJson(String raw) {
        int start = raw.indexOf('{');
        int end = raw.lastIndexOf('}');
        if (start >= 0 && end > start) {
            return raw.substring(start, end + 1);
        }
        return raw;
    }
}
