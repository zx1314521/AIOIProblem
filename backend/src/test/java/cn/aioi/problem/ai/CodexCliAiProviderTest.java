package cn.aioi.problem.ai;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class CodexCliAiProviderTest {
    @Test
    void resolvesWindowsNpmShimToCmdBeforeExtensionlessFile(@TempDir Path tempDir) throws Exception {
        Files.writeString(tempDir.resolve("codex"), "extensionless shim");
        Path cmd = tempDir.resolve("codex.cmd");
        Files.writeString(cmd, "@echo off");

        String resolved = CodexCliAiProvider.resolveExecutable("codex", tempDir.toString(), "Windows 11");

        assertThat(resolved).isEqualTo(cmd.toString());
    }

    @Test
    void skipsExtensionlessTemporaryShimAndKeepsSearching(@TempDir Path tempDir) throws Exception {
        Path temporaryShim = tempDir.resolve("tmp").resolve("arg0").resolve("codex-wrapper");
        Path npmBin = tempDir.resolve("npm");
        Files.createDirectories(temporaryShim);
        Files.createDirectories(npmBin);
        Files.writeString(temporaryShim.resolve("codex"), "temporary extensionless shim");
        Path cmd = npmBin.resolve("codex.cmd");
        Files.writeString(cmd, "@echo off");

        String resolved = CodexCliAiProvider.resolveExecutable(
                "codex",
                temporaryShim + ";" + npmBin,
                "Windows 11"
        );

        assertThat(resolved).isEqualTo(cmd.toString());
    }

    @Test
    void commandLineReadsPromptFromStdinAndWritesLastMessage(@TempDir Path tempDir) throws Exception {
        Path output = tempDir.resolve("last-message.txt");

        List<String> commandLine = CodexCliAiProvider.commandLine("codex", output);

        assertThat(commandLine).containsSubsequence(
                "exec",
                "--ignore-user-config",
                "--skip-git-repo-check",
                "--color",
                "never",
                "--output-last-message",
                output.toString(),
                "-"
        );
    }
}
