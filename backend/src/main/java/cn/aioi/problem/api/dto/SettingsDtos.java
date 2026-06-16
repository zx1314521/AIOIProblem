package cn.aioi.problem.api.dto;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;

public final class SettingsDtos {
    private SettingsDtos() {
    }

    public record AiSettingsRequest(
            @NotBlank String provider,
            String problemAnalysisProvider,
            String recommendationProvider,
            String dataGenerationProvider,
            String deepSeekApiKey,
            String deepSeekBaseUrl,
            String deepSeekModel,
            @Min(5) @Max(600) Integer deepSeekTimeoutSeconds,
            String codexCommand,
            String codexModel,
            @Min(5) @Max(1200) Integer codexTimeoutSeconds
    ) {
    }

    public record AiSettingsResponse(
            String provider,
            String problemAnalysisProvider,
            String recommendationProvider,
            String dataGenerationProvider,
            String deepSeekApiKey,
            String deepSeekBaseUrl,
            String deepSeekModel,
            int deepSeekTimeoutSeconds,
            String codexCommand,
            String codexModel,
            int codexTimeoutSeconds
    ) {
    }
}
