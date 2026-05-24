package cn.aioi.problem.ai;

import cn.aioi.problem.service.TagCatalogService;
import org.springframework.stereotype.Component;

import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.concurrent.TimeUnit;

@Component
public class CodexCliAiProvider {
    private final AiProperties properties;
    private final AiAssessmentParser parser;
    private final TagCatalogService tagCatalog;

    public CodexCliAiProvider(AiProperties properties, AiAssessmentParser parser, TagCatalogService tagCatalog) {
        this.properties = properties;
        this.parser = parser;
        this.tagCatalog = tagCatalog;
    }

    public AiAssessment assess(ProblemInput input) {
        Path outputFile = null;
        try {
            String command = properties.codex().command();
            if (command == null || command.isBlank()) {
                throw new IllegalStateException("Codex CLI 命令未配置");
            }
            outputFile = Files.createTempFile("aioi-codex-", ".txt");
            Process process = startProcess(command, prompt(input), outputFile);
            boolean finished = process.waitFor(properties.codex().timeoutSeconds(), TimeUnit.SECONDS);
            if (!finished) {
                process.destroyForcibly();
                throw new IllegalStateException("Codex CLI 调用超时");
            }
            String output = readCodexOutput(outputFile, process);
            return parser.parse(output);
        } catch (Exception exception) {
            throw new IllegalStateException("Codex CLI 调用失败: " + exception.getMessage(), exception);
        } finally {
            deleteIfExists(outputFile);
        }
    }

    public AiAssessment assess(ProblemInput input, AiRuntimeSettings settings) {
        Path outputFile = null;
        try {
            String command = settings.codexCommand();
            if (command == null || command.isBlank()) {
                throw new IllegalStateException("Codex CLI 命令未配置");
            }
            outputFile = Files.createTempFile("aioi-codex-", ".txt");
            Process process = startProcess(command, prompt(input), outputFile);
            boolean finished = process.waitFor(settings.codexTimeoutSeconds(), TimeUnit.SECONDS);
            if (!finished) {
                process.destroyForcibly();
                throw new IllegalStateException("Codex CLI 调用超时");
            }
            String output = readCodexOutput(outputFile, process);
            return parser.parse(output);
        } catch (Exception exception) {
            throw new IllegalStateException("Codex CLI 调用失败: " + exception.getMessage(), exception);
        } finally {
            deleteIfExists(outputFile);
        }
    }

    private String prompt(ProblemInput input) {
        return """
                请分析下面的信息学竞赛题目，只输出 JSON：
                {"difficulty":"入门|简单|CSPJ中等|CSPS提高|NOIP困难|地狱NOI","confidence":0.0,"tags":[],"hints":[],"reasoningSummary":""}
                """ + tagCatalog.promptText() + "\n题目：\n" + input.title() + "\n\n" + input.text();
    }

    private Process startProcess(String command, String prompt, Path outputFile) throws java.io.IOException {
        Process process = new ProcessBuilder(commandLine(command, outputFile))
                .redirectErrorStream(true)
                .start();
        try (var writer = process.outputWriter(StandardCharsets.UTF_8)) {
            writer.write(prompt);
        }
        return process;
    }

    private String readCodexOutput(Path outputFile, Process process) throws java.io.IOException {
        if (outputFile != null && Files.isRegularFile(outputFile)) {
            String lastMessage = Files.readString(outputFile, StandardCharsets.UTF_8).trim();
            if (!lastMessage.isBlank()) {
                return lastMessage;
            }
        }
        return new String(process.getInputStream().readAllBytes(), StandardCharsets.UTF_8);
    }

    private void deleteIfExists(Path outputFile) {
        if (outputFile == null) {
            return;
        }
        try {
            Files.deleteIfExists(outputFile);
        } catch (java.io.IOException ignored) {
            // Temporary output files are best-effort cleanup.
        }
    }

    static List<String> commandLine(String command, Path outputFile) {
        List<String> args = new ArrayList<>();
        args.add(resolveExecutable(command));
        args.add("exec");
        args.add("--ignore-user-config");
        args.add("--skip-git-repo-check");
        args.add("--color");
        args.add("never");
        args.add("--output-last-message");
        args.add(outputFile.toString());
        args.add("-");
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
