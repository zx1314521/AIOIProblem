package cn.aioi.problem.service;

import cn.aioi.problem.api.dto.ProblemDataDtos;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.stereotype.Component;

import java.util.HashSet;
import java.util.Set;

@Component
public class ProblemDataGenerationParser {
    private final ObjectMapper objectMapper;

    public ProblemDataGenerationParser(ObjectMapper objectMapper) {
        this.objectMapper = objectMapper;
    }

    public ProblemDataDtos.GeneratedData parse(String output) {
        String json = extractJson(output);
        try {
            ProblemDataDtos.GeneratedData data = objectMapper.readValue(json, ProblemDataDtos.GeneratedData.class);
            validate(data);
            return data;
        } catch (JsonProcessingException exception) {
            throw new IllegalArgumentException("AI 数据 JSON 解析失败: " + exception.getOriginalMessage(), exception);
        }
    }

    private String extractJson(String output) {
        if (output == null || output.isBlank()) {
            throw new IllegalArgumentException("AI 数据生成结果为空");
        }
        String trimmed = output.trim();
        if (trimmed.startsWith("```")) {
            int firstLineEnd = trimmed.indexOf('\n');
            int fenceEnd = trimmed.lastIndexOf("```");
            if (firstLineEnd >= 0 && fenceEnd > firstLineEnd) {
                return trimmed.substring(firstLineEnd + 1, fenceEnd).trim();
            }
        }
        int objectStart = trimmed.indexOf('{');
        int objectEnd = trimmed.lastIndexOf('}');
        if (objectStart >= 0 && objectEnd > objectStart) {
            return trimmed.substring(objectStart, objectEnd + 1);
        }
        throw new IllegalArgumentException("AI 数据生成结果未包含 JSON 对象");
    }

    private void validate(ProblemDataDtos.GeneratedData data) {
        if (data.stdCpp() == null || data.stdCpp().isBlank()) {
            throw new IllegalArgumentException("AI 数据缺少 stdCpp");
        }
        if (data.configYaml() == null || data.configYaml().isBlank()) {
            throw new IllegalArgumentException("AI 数据缺少 configYaml");
        }
        if (data.cases() == null || data.cases().size() != 25) {
            throw new IllegalArgumentException("AI 数据必须包含 25 组测试点");
        }
        Set<Integer> indexes = new HashSet<>();
        for (ProblemDataDtos.GeneratedCase item : data.cases()) {
            if (item.index() < 1 || item.index() > 25 || !indexes.add(item.index())) {
                throw new IllegalArgumentException("AI 数据测试点编号必须为 1 到 25 且不可重复");
            }
            if (item.input() == null || item.output() == null) {
                throw new IllegalArgumentException("AI 数据测试点必须包含 input 和 output");
            }
        }
    }
}
