package cn.aioi.problem.ai;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "aioi.ai")
public record AiProperties(String provider, DeepSeek deepseek, Codex codex) {
    public record DeepSeek(String apiKey, String baseUrl, String model, int timeoutSeconds) {
    }

    public record Codex(String command, String model, int timeoutSeconds) {
        public Codex(String command, int timeoutSeconds) {
            this(command, "", timeoutSeconds);
        }
    }
}
