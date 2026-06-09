package com.slideforge.api.settings;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.time.LocalDateTime;
import java.util.UUID;

@Entity
@Table(name = "ai_settings")
public class AiSettings {

    @Id
    private UUID id;

    @Column(name = "user_id", nullable = false, unique = true, length = 64)
    private String userId;

    @Column(nullable = false, length = 64)
    private String provider;

    @Column(name = "base_url", nullable = false, length = 512)
    private String baseUrl;

    @Column(name = "encrypted_api_key")
    private String encryptedApiKey;

    @Column(name = "api_key_mask", length = 64)
    private String apiKeyMask;

    @Column(nullable = false, length = 128)
    private String model;

    private Double temperature;

    @Column(name = "max_tokens")
    private Integer maxTokens;

    @Column(name = "created_at", nullable = false)
    private LocalDateTime createdAt;

    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt;

    protected AiSettings() {
    }

    public AiSettings(String userId) {
        LocalDateTime now = LocalDateTime.now();
        this.id = UUID.randomUUID();
        this.userId = userId;
        this.provider = "openai-compatible";
        this.baseUrl = "";
        this.model = "";
        this.temperature = 0.7;
        this.maxTokens = 4096;
        this.createdAt = now;
        this.updatedAt = now;
    }

    public String getProvider() {
        return provider;
    }

    public void setProvider(String provider) {
        this.provider = provider;
        touch();
    }

    public String getBaseUrl() {
        return baseUrl;
    }

    public void setBaseUrl(String baseUrl) {
        this.baseUrl = baseUrl;
        touch();
    }

    public String getEncryptedApiKey() {
        return encryptedApiKey;
    }

    public void setEncryptedApiKey(String encryptedApiKey) {
        this.encryptedApiKey = encryptedApiKey;
        touch();
    }

    public String getApiKeyMask() {
        return apiKeyMask;
    }

    public void setApiKeyMask(String apiKeyMask) {
        this.apiKeyMask = apiKeyMask;
        touch();
    }

    public String getModel() {
        return model;
    }

    public void setModel(String model) {
        this.model = model;
        touch();
    }

    public Double getTemperature() {
        return temperature;
    }

    public void setTemperature(Double temperature) {
        this.temperature = temperature;
        touch();
    }

    public Integer getMaxTokens() {
        return maxTokens;
    }

    public void setMaxTokens(Integer maxTokens) {
        this.maxTokens = maxTokens;
        touch();
    }

    private void touch() {
        this.updatedAt = LocalDateTime.now();
    }
}

