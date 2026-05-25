package cn.aioi.problem.ai;

public record AiRuntimeSettings(
        String provider,
        String deepSeekApiKey,
        String deepSeekBaseUrl,
        String deepSeekModel,
        int deepSeekTimeoutSeconds,
        String codexCommand,
        int codexTimeoutSeconds
) {
    public String providerLabel() {
        return switch (provider) {
            case "deepseek" -> "DeepSeek API";
            case "codex" -> "Codex CLI";
            case "mock" -> "本地规则模型";
            default -> "AI Provider";
        };
    }
}
