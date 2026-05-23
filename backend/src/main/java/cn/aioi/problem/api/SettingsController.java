package cn.aioi.problem.api;

import cn.aioi.problem.api.dto.SettingsDtos;
import cn.aioi.problem.service.AiSettingsService;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/settings")
public class SettingsController {
    private final AiSettingsService aiSettingsService;

    public SettingsController(AiSettingsService aiSettingsService) {
        this.aiSettingsService = aiSettingsService;
    }

    @GetMapping("/ai")
    SettingsDtos.AiSettingsResponse getAiSettings() {
        return aiSettingsService.getSettings();
    }

    @PutMapping("/ai")
    SettingsDtos.AiSettingsResponse updateAiSettings(@Valid @RequestBody SettingsDtos.AiSettingsRequest request) {
        return aiSettingsService.update(request);
    }
}

