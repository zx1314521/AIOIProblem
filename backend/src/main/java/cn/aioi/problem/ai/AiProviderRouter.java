package cn.aioi.problem.ai;

import org.springframework.stereotype.Service;
import org.springframework.context.annotation.Primary;

@Service
@Primary
public class AiProviderRouter implements AiProvider {
    private final AiProperties properties;
    private final RuleBasedAiProvider ruleBased;
    private final DeepSeekAiProvider deepSeek;
    private final CodexCliAiProvider codexCli;

    public AiProviderRouter(AiProperties properties, RuleBasedAiProvider ruleBased,
                            DeepSeekAiProvider deepSeek, CodexCliAiProvider codexCli) {
        this.properties = properties;
        this.ruleBased = ruleBased;
        this.deepSeek = deepSeek;
        this.codexCli = codexCli;
    }

    @Override
    public AiAssessment assess(ProblemInput input) {
        try {
            String provider = properties.provider() == null ? "mock" : properties.provider().trim().toLowerCase();
            return switch (provider) {
                case "deepseek" -> deepSeek.assess(input);
                case "codex" -> codexCli.assess(input);
                default -> ruleBased.assess(input);
            };
        } catch (RuntimeException exception) {
            AiAssessment fallback = ruleBased.assess(input);
            return new AiAssessment(
                    fallback.difficulty(),
                    Math.min(fallback.confidence(), 0.55),
                    fallback.tags(),
                    fallback.hints(),
                    "AI Provider 不可用，已回退到本地规则模型：" + exception.getMessage()
            );
        }
    }
}
