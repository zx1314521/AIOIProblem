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
        AiRuntimeSettings settings = settingsService.runtimeSettings();
        return switch (settings.provider()) {
            case "deepseek" -> deepSeek.assess(input, settings);
            case "codex" -> codexCli.assess(input, settings);
            default -> ruleBased.assess(input);
        };
    }
}
