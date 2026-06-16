package cn.aioi.problem.service;

import cn.aioi.problem.api.dto.ProblemDataDtos;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Duration;
import java.util.Comparator;
import java.util.List;
import java.util.concurrent.TimeUnit;

@Service
public class CppExecutionService {
    private static final int COMPILE_TIMEOUT_SECONDS = 10;
    private static final int RUN_TIMEOUT_SECONDS = 2;
    private static final int OUTPUT_LIMIT = 1_000_000;

    public ProblemDataDtos.CodeRunResponse runDebug(String code, String input) {
        long started = System.nanoTime();
        Path dir = null;
        try {
            dir = Files.createTempDirectory("aioi-cpp-");
            CompileResult compiled = compile(dir, code);
            if (!compiled.ok()) {
                return new ProblemDataDtos.CodeRunResponse("CE", "", compiled.log(), null, elapsedMs(started), List.of());
            }
            RunResult result = runExecutable(dir, compiled.executable(), input == null ? "" : input);
            return new ProblemDataDtos.CodeRunResponse(
                    result.status(),
                    result.stdout(),
                    result.stderr(),
                    result.exitCode(),
                    result.durationMs(),
                    List.of()
            );
        } catch (IOException exception) {
            return new ProblemDataDtos.CodeRunResponse("RE", "", exception.getMessage(), null, elapsedMs(started), List.of());
        } finally {
            deleteRecursively(dir);
        }
    }

    public ProblemDataDtos.CodeRunResponse runCases(String code, List<ProblemDataDtos.DataCaseResponse> cases) {
        long started = System.nanoTime();
        Path dir = null;
        try {
            dir = Files.createTempDirectory("aioi-cpp-");
            CompileResult compiled = compile(dir, code);
            if (!compiled.ok()) {
                return new ProblemDataDtos.CodeRunResponse("CE", "", compiled.log(), null, elapsedMs(started), List.of());
            }
            Path executionDir = dir;
            List<ProblemDataDtos.CaseRunResponse> results = cases.stream()
                    .map(testCase -> runCase(executionDir, compiled.executable(), testCase))
                    .toList();
            String status = aggregateStatus(results);
            return new ProblemDataDtos.CodeRunResponse(status, "", "", null, elapsedMs(started), results);
        } catch (IOException exception) {
            return new ProblemDataDtos.CodeRunResponse("RE", "", exception.getMessage(), null, elapsedMs(started), List.of());
        } finally {
            deleteRecursively(dir);
        }
    }

    private ProblemDataDtos.CaseRunResponse runCase(Path dir, Path executable, ProblemDataDtos.DataCaseResponse testCase) {
        RunResult result = runExecutable(dir, executable, testCase.input());
        String status = result.status();
        if ("OK".equals(status)) {
            status = normalize(result.stdout()).equals(normalize(testCase.output())) ? "AC" : "WA";
        }
        return new ProblemDataDtos.CaseRunResponse(
                testCase.index(),
                status,
                result.stdout(),
                result.stderr(),
                testCase.output(),
                result.durationMs()
        );
    }

    private CompileResult compile(Path dir, String code) throws IOException {
        Path source = dir.resolve("main.cpp");
        Path executable = executablePath(dir);
        Path log = dir.resolve("compile.log");
        Files.writeString(source, code == null ? "" : code, StandardCharsets.UTF_8);
        Process process = new ProcessBuilder("g++", "-std=c++17", source.toString(), "-O2", "-o", executable.toString())
                .redirectErrorStream(true)
                .redirectOutput(log.toFile())
                .start();
        boolean finished = waitFor(process, COMPILE_TIMEOUT_SECONDS);
        if (!finished) {
            process.destroyForcibly();
            return new CompileResult(false, executable, "Compile timed out");
        }
        String output = readLimited(log);
        return new CompileResult(process.exitValue() == 0, executable, output);
    }

    private RunResult runExecutable(Path dir, Path executable, String input) {
        long started = System.nanoTime();
        try {
            Path stdin = Files.createTempFile(dir, "stdin-", ".txt");
            Path stdout = Files.createTempFile(dir, "stdout-", ".txt");
            Path stderr = Files.createTempFile(dir, "stderr-", ".txt");
            Files.writeString(stdin, input == null ? "" : input, StandardCharsets.UTF_8);
            Process process = new ProcessBuilder(executable.toString())
                    .directory(dir.toFile())
                    .redirectInput(stdin.toFile())
                    .redirectOutput(stdout.toFile())
                    .redirectError(stderr.toFile())
                    .start();
            boolean finished = waitFor(process, RUN_TIMEOUT_SECONDS);
            if (!finished) {
                process.destroyForcibly();
                return new RunResult("TLE", readLimited(stdout), readLimited(stderr), null, elapsedMs(started));
            }
            String status = process.exitValue() == 0 ? "OK" : "RE";
            return new RunResult(status, readLimited(stdout), readLimited(stderr), process.exitValue(), elapsedMs(started));
        } catch (IOException exception) {
            return new RunResult("RE", "", exception.getMessage(), null, elapsedMs(started));
        }
    }

    private boolean waitFor(Process process, int timeoutSeconds) {
        try {
            return process.waitFor(timeoutSeconds, TimeUnit.SECONDS);
        } catch (InterruptedException exception) {
            Thread.currentThread().interrupt();
            process.destroyForcibly();
            return false;
        }
    }

    private Path executablePath(Path dir) {
        String os = System.getProperty("os.name", "").toLowerCase();
        return dir.resolve(os.contains("win") ? "main.exe" : "main");
    }

    private String aggregateStatus(List<ProblemDataDtos.CaseRunResponse> results) {
        if (results.stream().anyMatch(result -> "TLE".equals(result.status()))) {
            return "TLE";
        }
        if (results.stream().anyMatch(result -> "RE".equals(result.status()))) {
            return "RE";
        }
        if (results.stream().anyMatch(result -> "WA".equals(result.status()))) {
            return "WA";
        }
        return "AC";
    }

    private String normalize(String value) {
        return value == null ? "" : value.strip();
    }

    private long elapsedMs(long started) {
        return Duration.ofNanos(System.nanoTime() - started).toMillis();
    }

    private String readLimited(Path path) {
        try {
            if (!Files.isRegularFile(path)) {
                return "";
            }
            byte[] bytes = Files.readAllBytes(path);
            int length = Math.min(bytes.length, OUTPUT_LIMIT);
            String value = new String(bytes, 0, length, StandardCharsets.UTF_8).replace("\r\n", "\n");
            return bytes.length > OUTPUT_LIMIT ? value + "\n[output truncated]" : value;
        } catch (IOException exception) {
            return exception.getMessage();
        }
    }

    private void deleteRecursively(Path dir) {
        if (dir == null || !Files.exists(dir)) {
            return;
        }
        try (var paths = Files.walk(dir)) {
            paths.sorted(Comparator.reverseOrder()).forEach(path -> {
                try {
                    Files.deleteIfExists(path);
                } catch (IOException ignored) {
                    // Temporary execution files are best-effort cleanup.
                }
            });
        } catch (IOException ignored) {
            // Temporary execution files are best-effort cleanup.
        }
    }

    private record CompileResult(boolean ok, Path executable, String log) {
    }

    private record RunResult(String status, String stdout, String stderr, Integer exitCode, long durationMs) {
    }
}
