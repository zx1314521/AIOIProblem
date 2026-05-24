package cn.aioi.problem.ai;

import org.springframework.stereotype.Component;

import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
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
            Process process = startProcess(command, prompt(input));
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
            Process process = startProcess(command, prompt(input));
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

    private Process startProcess(String command, String prompt) throws java.io.IOException {
        return new ProcessBuilder(commandLine(command, prompt))
                .redirectErrorStream(true)
                .start();
    }

    static List<String> commandLine(String command, String prompt) {
        List<String> args = new ArrayList<>();
        args.add(resolveExecutable(command));
        args.add("exec");
        args.add(prompt);
        return args;
    }

    static String resolveExecutable(String command) {
        return resolveExecutable(command, System.getenv("PATH"), System.getProperty("os.name", ""));
    }

    static String resolveExecutable(String command, String pathValue, String osName) {
        if (!isWindows(osName)) {
            return command;
        }
        Path direct = Paths.get(command);
        if (command.contains("\\") || command.contains("/")) {
            String resolved = resolveWindowsSibling(direct);
            return resolved.isBlank() ? command : resolved;
        }
        if (pathValue == null || pathValue.isBlank()) {
            return command;
        }
        for (String entry : pathValue.split(";")) {
            if (entry.isBlank()) {
                continue;
            }
            String resolved = resolveWindowsSibling(Paths.get(entry).resolve(command));
            if (!resolved.isBlank()) {
                return resolved;
            }
        }
        return command;
    }

    private static String resolveWindowsSibling(Path candidate) {
        String value = candidate.toString();
        String lower = value.toLowerCase(Locale.ROOT);
        if ((lower.endsWith(".cmd") || lower.endsWith(".exe") || lower.endsWith(".bat")) && Files.isRegularFile(candidate)) {
            return value;
        }
        for (String extension : List.of(".cmd", ".exe", ".bat")) {
            Path withExtension = Paths.get(value + extension);
            if (Files.isRegularFile(withExtension)) {
                return withExtension.toString();
            }
        }
        return "";
    }

    private static boolean isWindows(String osName) {
        return osName.toLowerCase(Locale.ROOT).contains("win");
    }
}
