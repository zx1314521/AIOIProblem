package cn.aioi.problem.ai;

import org.springframework.stereotype.Service;
import org.springframework.context.annotation.Primary;
import cn.aioi.problem.service.AiSettingsService;

@Service
@Primary
public class AiProviderRouter implements AiProvider {
    private final AiSettingsService settingsService;
    private final RuleBasedAiProvider ruleBased;
    private final DeepSeekAiProvider deepSeek;
    private final CodexCliAiProvider codexCli;

    public AiProviderRouter(AiSettingsService settingsService, RuleBasedAiProvider ruleBased,
                            DeepSeekAiProvider deepSeek, CodexCliAiProvider codexCli) {
        this.settingsService = settingsService;
        this.ruleBased = ruleBased;
        this.deepSeek = deepSeek;
        this.codexCli = codexCli;
    }

    @Override
    public AiAssessment assess(ProblemInput input) {
        try {
            AiRuntimeSettings settings = settingsService.runtimeSettings();
            return switch (settings.provider()) {
                case "deepseek" -> deepSeek.assess(input, settings);
                case "codex" -> codexCli.assess(input, settings);
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
