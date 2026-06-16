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
        Path logFile = null;
        try {
            String command = properties.codex().command();
            if (command == null || command.isBlank()) {
                throw new IllegalStateException("Codex CLI command is not configured");
            }
            outputFile = Files.createTempFile("aioi-codex-", ".txt");
            logFile = Files.createTempFile("aioi-codex-log-", ".txt");
            String model = properties.codex() == null ? "" : properties.codex().model();
            Process process = startProcess(command, model, prompt(input), outputFile, logFile);
            boolean finished = process.waitFor(properties.codex().timeoutSeconds(), TimeUnit.SECONDS);
            if (!finished) {
                process.destroyForcibly();
                throw new IllegalStateException("Codex CLI timed out: " + logTail(logFile));
            }
            if (process.exitValue() != 0) {
                throw new IllegalStateException("Codex CLI exited with " + process.exitValue() + ": " + logTail(logFile));
            }
            String output = readCodexOutput(outputFile, logFile);
            return parser.parse(output);
        } catch (Exception exception) {
            throw new IllegalStateException("Codex CLI call failed: " + exception.getMessage(), exception);
        } finally {
            deleteIfExists(outputFile);
            deleteIfExists(logFile);
        }
    }

    public AiAssessment assess(ProblemInput input, AiRuntimeSettings settings) {
        Path outputFile = null;
        Path logFile = null;
        try {
            String command = settings.codexCommand();
            if (command == null || command.isBlank()) {
                throw new IllegalStateException("Codex CLI command is not configured");
            }
            outputFile = Files.createTempFile("aioi-codex-", ".txt");
            logFile = Files.createTempFile("aioi-codex-log-", ".txt");
            Process process = startProcess(command, settings.codexModel(), prompt(input), outputFile, logFile);
            boolean finished = process.waitFor(settings.codexTimeoutSeconds(), TimeUnit.SECONDS);
            if (!finished) {
                process.destroyForcibly();
                throw new IllegalStateException("Codex CLI timed out: " + logTail(logFile));
            }
            if (process.exitValue() != 0) {
                throw new IllegalStateException("Codex CLI exited with " + process.exitValue() + ": " + logTail(logFile));
            }
            String output = readCodexOutput(outputFile, logFile);
            return parser.parse(output);
        } catch (Exception exception) {
            throw new IllegalStateException("Codex CLI call failed: " + exception.getMessage(), exception);
        } finally {
            deleteIfExists(outputFile);
            deleteIfExists(logFile);
        }
    }

    public String polishProblemStatement(ProblemInput input, AiRuntimeSettings settings) {
        Path outputFile = null;
        Path logFile = null;
        try {
            String command = settings.codexCommand();
            if (command == null || command.isBlank()) {
                throw new IllegalStateException("Codex CLI command is not configured");
            }
            outputFile = Files.createTempFile("aioi-codex-polish-", ".txt");
            logFile = Files.createTempFile("aioi-codex-polish-log-", ".txt");
            Process process = startProcess(command, settings.codexModel(), polishPrompt(input), outputFile, logFile);
            boolean finished = process.waitFor(settings.codexTimeoutSeconds(), TimeUnit.SECONDS);
            if (!finished) {
                process.destroyForcibly();
                throw new IllegalStateException("Codex CLI timed out: " + logTail(logFile));
            }
            if (process.exitValue() != 0) {
                throw new IllegalStateException("Codex CLI exited with " + process.exitValue() + ": " + logTail(logFile));
            }
            return readCodexOutput(outputFile, logFile).trim();
        } catch (Exception exception) {
            throw new IllegalStateException("Codex CLI statement polishing failed: " + exception.getMessage(), exception);
        } finally {
            deleteIfExists(outputFile);
            deleteIfExists(logFile);
        }
    }

    public String generateTestData(ProblemInput input, AiRuntimeSettings settings) {
        Path outputFile = null;
        Path logFile = null;
        try {
            String command = settings.codexCommand();
            if (command == null || command.isBlank()) {
                throw new IllegalStateException("Codex CLI command is not configured");
            }
            outputFile = Files.createTempFile("aioi-codex-testdata-", ".txt");
            logFile = Files.createTempFile("aioi-codex-testdata-log-", ".txt");
            Process process = startProcess(command, settings.codexModel(), testDataPrompt(input), outputFile, logFile);
            boolean finished = process.waitFor(settings.codexTimeoutSeconds(), TimeUnit.SECONDS);
            if (!finished) {
                process.destroyForcibly();
                throw new IllegalStateException("Codex CLI timed out: " + logTail(logFile));
            }
            if (process.exitValue() != 0) {
                throw new IllegalStateException("Codex CLI exited with " + process.exitValue() + ": " + logTail(logFile));
            }
            return readCodexOutput(outputFile, logFile).trim();
        } catch (Exception exception) {
            throw new IllegalStateException("Codex CLI test data generation failed: " + exception.getMessage(), exception);
        } finally {
            deleteIfExists(outputFile);
            deleteIfExists(logFile);
        }
    }

    private String prompt(ProblemInput input) {
        return """
                Analyze the following competitive programming problem and output JSON only.
                Required JSON shape:
                {"difficulty":"ENTRY|EASY|CSPJ_MEDIUM|CSPS_ADVANCED|NOIP_HARD|NOI_HELL","confidence":0.0,"tags":[],"hints":[],"reasoningSummary":""}
                Tags should come from the catalog below when possible.
                """ + tagCatalog.promptText() + "\nTitle:\n" + input.title() + "\n\nStatement:\n" + input.text();
    }

    private String polishPrompt(ProblemInput input) {
        return """
                Translate and clean the following competitive programming statement into polished Chinese.
                Requirements:
                1. Output only the cleaned statement body. Do not output explanations, Markdown code fences, or JSON.
                2. Preserve meaning, input/output format, samples, constraints, math symbols, and variable names.
                3. Remove web navigation, buttons, ads, duplicated blank lines, and irrelevant page status text.
                4. If the original text is already Chinese, only improve formatting and remove noise.
                Title:
                """ + input.title() + "\n\nRaw statement:\n" + input.text();
    }

    private String testDataPrompt(ProblemInput input) {
        return """
                You are generating competitive-programming test data for an existing problem.
                Follow the ojimport "only generate test data" workflow:
                - Infer input format, output format, constraints, time/memory intent, and algorithm type from the statement.
                - Produce a correct C++17 reference solution as stdCpp.
                - Produce exactly 25 paired test cases.
                - Cases 1-2 should be samples when present, otherwise minimal valid cases.
                - Cases 3-8 cover small scale and boundary properties.
                - Cases 9-11 are hack cases for common wrong solutions.
                - Cases 12-20 cover medium/large stress.
                - Cases 21-25 are mixed random regression cases.
                - Produce configYaml with type/time/memory and all cases.
                Output JSON only, no Markdown, with this exact shape:
                {"stdCpp":"","configYaml":"","notes":"","cases":[{"index":1,"input":"","output":""}]}
                The cases array must contain indexes 1 through 25 exactly once.
                Title:
                """ + input.title() + "\n\nStatement:\n" + input.text();
    }

    private Process startProcess(String command, String model, String prompt, Path outputFile, Path logFile) throws java.io.IOException {
        Process process = new ProcessBuilder(commandLine(command, model, outputFile))
                .redirectErrorStream(true)
                .redirectOutput(logFile.toFile())
                .start();
        try (var writer = process.outputWriter(StandardCharsets.UTF_8)) {
            writer.write(prompt);
        }
        return process;
    }

    private String readCodexOutput(Path outputFile, Path logFile) throws java.io.IOException {
        if (outputFile != null && Files.isRegularFile(outputFile)) {
            String lastMessage = Files.readString(outputFile, StandardCharsets.UTF_8).trim();
            if (!lastMessage.isBlank()) {
                return lastMessage;
            }
        }
        return Files.isRegularFile(logFile) ? Files.readString(logFile, StandardCharsets.UTF_8) : "";
    }

    private String logTail(Path logFile) throws java.io.IOException {
        if (logFile == null || !Files.isRegularFile(logFile)) {
            return "no execution log";
        }
        String log = Files.readString(logFile, StandardCharsets.UTF_8).trim();
        if (log.isBlank()) {
            return "no execution log";
        }
        int maxLength = 1200;
        return log.length() <= maxLength ? log : log.substring(log.length() - maxLength);
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
        return commandLine(command, "", outputFile);
    }

    static List<String> commandLine(String command, String model, Path outputFile) {
        List<String> args = new ArrayList<>();
        args.add(resolveExecutable(command));
        args.add("exec");
        args.add("--ignore-user-config");
        args.add("--skip-git-repo-check");
        args.add("--color");
        args.add("never");
        if (model != null && !model.isBlank()) {
            args.add("--model");
            args.add(model.trim());
        }
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
