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
        try {
            return switch (settings.provider()) {
                case "deepseek" -> deepSeek.assess(input, settings);
                case "codex" -> codexCli.assess(input, settings);
                default -> ruleBased.assess(input);
            };
        } catch (RuntimeException exception) {
            AiAssessment fallback = ruleBased.assess(input);
            return new AiAssessment(
                    fallback.difficulty(),
                    Math.min(fallback.confidence(), 0.62),
                    fallback.tags(),
                    fallback.hints(),
                    settings.providerLabel() + " 调用失败，已使用本地规则模型兜底：" + fallback.reasoningSummary()
            );
        }
    }
}
