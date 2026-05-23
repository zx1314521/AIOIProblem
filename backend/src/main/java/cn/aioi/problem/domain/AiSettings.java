package cn.aioi.problem.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

@Entity
@Table(name = "ai_settings")
public class AiSettings {
    @Id
    private Long id;

    @Column(nullable = false, length = 24)
    private String provider;

    private String deepSeekApiKey;
    private String deepSeekBaseUrl;
    private String deepSeekModel;
    private Integer deepSeekTimeoutSeconds;

    private String codexCommand;
    private Integer codexTimeoutSeconds;

    protected AiSettings() {
    }

    public AiSettings(Long id, String provider, String deepSeekApiKey, String deepSeekBaseUrl, String deepSeekModel,
                      Integer deepSeekTimeoutSeconds, String codexCommand, Integer codexTimeoutSeconds) {
        this.id = id;
        this.provider = provider;
        this.deepSeekApiKey = deepSeekApiKey;
        this.deepSeekBaseUrl = deepSeekBaseUrl;
        this.deepSeekModel = deepSeekModel;
        this.deepSeekTimeoutSeconds = deepSeekTimeoutSeconds;
        this.codexCommand = codexCommand;
        this.codexTimeoutSeconds = codexTimeoutSeconds;
    }

    public void update(String provider, String deepSeekApiKey, String deepSeekBaseUrl, String deepSeekModel,
                       Integer deepSeekTimeoutSeconds, String codexCommand, Integer codexTimeoutSeconds) {
        this.provider = provider;
        this.deepSeekApiKey = deepSeekApiKey;
        this.deepSeekBaseUrl = deepSeekBaseUrl;
        this.deepSeekModel = deepSeekModel;
        this.deepSeekTimeoutSeconds = deepSeekTimeoutSeconds;
        this.codexCommand = codexCommand;
        this.codexTimeoutSeconds = codexTimeoutSeconds;
    }

    public Long getId() {
        return id;
    }

    public String getProvider() {
        return provider;
    }

    public String getDeepSeekApiKey() {
        return deepSeekApiKey;
    }

    public String getDeepSeekBaseUrl() {
        return deepSeekBaseUrl;
    }

    public String getDeepSeekModel() {
        return deepSeekModel;
    }

    public Integer getDeepSeekTimeoutSeconds() {
        return deepSeekTimeoutSeconds;
    }

    public String getCodexCommand() {
        return codexCommand;
    }

    public Integer getCodexTimeoutSeconds() {
        return codexTimeoutSeconds;
    }
}

