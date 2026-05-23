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
}

