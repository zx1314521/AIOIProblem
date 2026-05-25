package cn.aioi.problem.service;

import cn.aioi.problem.ai.AiProperties;
import cn.aioi.problem.ai.AiRuntimeSettings;
import cn.aioi.problem.ai.AiTaskType;
import cn.aioi.problem.api.dto.SettingsDtos;
import cn.aioi.problem.domain.AiSettings;
import cn.aioi.problem.repository.AiSettingsRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class AiSettingsService {
    private static final long SETTINGS_ID = 1L;

    private final AiSettingsRepository settingsRepository;
    private final AiProperties properties;

    public AiSettingsService(AiSettingsRepository settingsRepository, AiProperties properties) {
        this.settingsRepository = settingsRepository;
        this.properties = properties;
    }

    @Transactional(readOnly = true)
    public SettingsDtos.AiSettingsResponse getSettings() {
        return toResponse(loadSettings());
    }

    @Transactional
    public SettingsDtos.AiSettingsResponse update(SettingsDtos.AiSettingsRequest request) {
        AiSettings settings = loadSettings();
        String defaultProvider = normalizeProvider(request.provider());
        settings.update(
                defaultProvider,
                normalizeProvider(defaultString(request.problemAnalysisProvider(), defaultProvider)),
                normalizeProvider(defaultString(request.recommendationProvider(), defaultProvider)),
                clean(request.deepSeekApiKey()),
                defaultString(request.deepSeekBaseUrl(), defaultDeepSeekBaseUrl()),
                defaultString(request.deepSeekModel(), defaultDeepSeekModel()),
                request.deepSeekTimeoutSeconds() == null ? defaultDeepSeekTimeout() : request.deepSeekTimeoutSeconds(),
                defaultString(request.codexCommand(), defaultCodexCommand()),
                request.codexTimeoutSeconds() == null ? defaultCodexTimeout() : request.codexTimeoutSeconds()
        );
        return toResponse(settingsRepository.save(settings));
    }

    @Transactional(readOnly = true)
    public AiRuntimeSettings runtimeSettings() {
        return runtimeSettings(AiTaskType.PROBLEM_ANALYSIS);
    }

    @Transactional(readOnly = true)
    public AiRuntimeSettings runtimeSettings(AiTaskType taskType) {
        AiSettings settings = loadSettings();
        String provider = providerForTask(settings, taskType);
        return new AiRuntimeSettings(
                provider,
                clean(settings.getDeepSeekApiKey()),
                defaultString(settings.getDeepSeekBaseUrl(), defaultDeepSeekBaseUrl()),
                defaultString(settings.getDeepSeekModel(), defaultDeepSeekModel()),
                settings.getDeepSeekTimeoutSeconds() == null ? defaultDeepSeekTimeout() : settings.getDeepSeekTimeoutSeconds(),
                defaultString(settings.getCodexCommand(), defaultCodexCommand()),
                settings.getCodexTimeoutSeconds() == null ? defaultCodexTimeout() : settings.getCodexTimeoutSeconds()
        );
    }

    private AiSettings loadSettings() {
        return settingsRepository.findById(SETTINGS_ID).orElseGet(() -> settingsRepository.save(new AiSettings(
                SETTINGS_ID,
                normalizeProvider(properties.provider()),
                defaultDeepSeekApiKey(),
                defaultDeepSeekBaseUrl(),
                defaultDeepSeekModel(),
                defaultDeepSeekTimeout(),
                defaultCodexCommand(),
                defaultCodexTimeout()
        )));
    }

    private SettingsDtos.AiSettingsResponse toResponse(AiSettings settings) {
        String defaultProvider = normalizeProvider(settings.getProvider());
        AiRuntimeSettings runtime = new AiRuntimeSettings(
                defaultProvider,
                clean(settings.getDeepSeekApiKey()),
                defaultString(settings.getDeepSeekBaseUrl(), defaultDeepSeekBaseUrl()),
                defaultString(settings.getDeepSeekModel(), defaultDeepSeekModel()),
                settings.getDeepSeekTimeoutSeconds() == null ? defaultDeepSeekTimeout() : settings.getDeepSeekTimeoutSeconds(),
                defaultString(settings.getCodexCommand(), defaultCodexCommand()),
                settings.getCodexTimeoutSeconds() == null ? defaultCodexTimeout() : settings.getCodexTimeoutSeconds()
        );
        return new SettingsDtos.AiSettingsResponse(
                runtime.provider(),
                normalizeProvider(defaultString(settings.getProblemAnalysisProvider(), defaultProvider)),
                normalizeProvider(defaultString(settings.getRecommendationProvider(), defaultProvider)),
                runtime.deepSeekApiKey(),
                runtime.deepSeekBaseUrl(),
                runtime.deepSeekModel(),
                runtime.deepSeekTimeoutSeconds(),
                runtime.codexCommand(),
                runtime.codexTimeoutSeconds()
        );
    }

    private String providerForTask(AiSettings settings, AiTaskType taskType) {
        String defaultProvider = normalizeProvider(settings.getProvider());
        String configured = switch (taskType) {
            case PROBLEM_ANALYSIS -> settings.getProblemAnalysisProvider();
            case RECOMMENDATION -> settings.getRecommendationProvider();
        };
        return normalizeProvider(defaultString(configured, defaultProvider));
    }

    private String normalizeProvider(String provider) {
        String value = defaultString(provider, "codex").toLowerCase();
        return switch (value) {
            case "deepseek", "mock", "codex" -> value;
            default -> "codex";
        };
    }

    private String defaultDeepSeekApiKey() {
        return properties.deepseek() == null ? "" : clean(properties.deepseek().apiKey());
    }

    private String defaultDeepSeekBaseUrl() {
        return properties.deepseek() == null ? "https://api.deepseek.com/chat/completions" : defaultString(properties.deepseek().baseUrl(), "https://api.deepseek.com/chat/completions");
    }

    private String defaultDeepSeekModel() {
        return properties.deepseek() == null ? "deepseek-chat" : defaultString(properties.deepseek().model(), "deepseek-chat");
    }

    private int defaultDeepSeekTimeout() {
        return properties.deepseek() == null ? 45 : Math.max(5, properties.deepseek().timeoutSeconds());
    }

    private String defaultCodexCommand() {
        return properties.codex() == null ? "codex" : defaultString(properties.codex().command(), "codex");
    }

    private int defaultCodexTimeout() {
        return properties.codex() == null ? 180 : Math.max(180, properties.codex().timeoutSeconds());
    }

    private String defaultString(String value, String fallback) {
        return value == null || value.isBlank() ? fallback : value.trim();
    }

    private String clean(String value) {
        return value == null ? "" : value.trim();
    }
}
