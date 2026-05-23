package cn.aioi.problem.ai;

import org.springframework.stereotype.Component;

import java.nio.charset.StandardCharsets;
import java.util.concurrent.TimeUnit;

@Component
public class CodexCliAiProvider {
    private final AiProperties properties;
    private final AiAssessmentParser parser;

    public CodexCliAiProvider(AiProperties properties, AiAssessmentParser parser) {
        this.properties = properties;
        this.parser = parser;
    }

    public AiAssessment assess(ProblemInput input) {
        try {
            String command = properties.codex().command();
            if (command == null || command.isBlank()) {
                throw new IllegalStateException("Codex CLI 命令未配置");
            }
            Process process = new ProcessBuilder(command, "exec", prompt(input))
                    .redirectErrorStream(true)
                    .start();
            boolean finished = process.waitFor(properties.codex().timeoutSeconds(), TimeUnit.SECONDS);
            if (!finished) {
                process.destroyForcibly();
                throw new IllegalStateException("Codex CLI 调用超时");
            }
            String output = new String(process.getInputStream().readAllBytes(), StandardCharsets.UTF_8);
            return parser.parse(output);
        } catch (Exception exception) {
            throw new IllegalStateException("Codex CLI 调用失败: " + exception.getMessage(), exception);
        }
    }

    public AiAssessment assess(ProblemInput input, AiRuntimeSettings settings) {
        try {
            String command = settings.codexCommand();
            if (command == null || command.isBlank()) {
                throw new IllegalStateException("Codex CLI 命令未配置");
            }
            Process process = new ProcessBuilder(command, "exec", prompt(input))
                    .redirectErrorStream(true)
                    .start();
            boolean finished = process.waitFor(settings.codexTimeoutSeconds(), TimeUnit.SECONDS);
            if (!finished) {
                process.destroyForcibly();
                throw new IllegalStateException("Codex CLI 调用超时");
            }
            String output = new String(process.getInputStream().readAllBytes(), StandardCharsets.UTF_8);
            return parser.parse(output);
        } catch (Exception exception) {
            throw new IllegalStateException("Codex CLI 调用失败: " + exception.getMessage(), exception);
        }
    }

    private String prompt(ProblemInput input) {
        return """
                请分析下面的信息学竞赛题目，只输出 JSON：
                {"difficulty":"入门|简单|CSPJ中等|CSPS提高|NOIP困难|地狱NOI","confidence":0.0,"tags":[],"hints":[],"reasoningSummary":""}
                题目：
                """ + input.title() + "\n\n" + input.text();
    }
}
