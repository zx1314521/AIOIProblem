package cn.aioi.problem.ai;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.nio.file.Files;
import java.nio.file.Path;

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
}
