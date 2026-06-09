package com.slideforge.api.settings;

import com.slideforge.api.ai.provider.AiChatRequest;
import com.slideforge.api.ai.provider.AiMessage;
import com.slideforge.api.ai.provider.AiProviderClient;
import com.slideforge.api.security.SecretCryptoService;
import com.slideforge.api.settings.dto.AiConnectionTestResponse;
import com.slideforge.api.settings.dto.AiSettingsResponse;
import com.slideforge.api.settings.dto.UpdateAiSettingsRequest;
import java.util.List;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

@Service
public class AiSettingsService {

    private static final String LOCAL_USER_ID = "local-user";

    private final AiSettingsRepository aiSettingsRepository;
    private final AiProviderClient aiProviderClient;
    private final SecretCryptoService secretCryptoService;

    public AiSettingsService(
            AiSettingsRepository aiSettingsRepository,
            AiProviderClient aiProviderClient,
            SecretCryptoService secretCryptoService
    ) {
        this.aiSettingsRepository = aiSettingsRepository;
        this.aiProviderClient = aiProviderClient;
        this.secretCryptoService = secretCryptoService;
    }

    public AiSettingsResponse getSettings() {
        return toResponse(getOrCreateSettings());
    }

    public AiSettingsResponse updateSettings(UpdateAiSettingsRequest request) {
        AiSettings settings = getOrCreateSettings();
        settings.setProvider(request.provider());
        settings.setBaseUrl(request.baseUrl());
        settings.setModel(request.model());
        settings.setTemperature(request.temperature() == null ? settings.getTemperature() : request.temperature());
        settings.setMaxTokens(request.maxTokens() == null ? settings.getMaxTokens() : request.maxTokens());

        if (StringUtils.hasText(request.apiKey())) {
            settings.setEncryptedApiKey(secretCryptoService.encrypt(request.apiKey()));
            settings.setApiKeyMask(maskApiKey(request.apiKey()));
        }

        return toResponse(aiSettingsRepository.save(settings));
    }

    public AiSettingsResponse deleteApiKey() {
        AiSettings settings = getOrCreateSettings();
        settings.setEncryptedApiKey(null);
        settings.setApiKeyMask("");
        return toResponse(aiSettingsRepository.save(settings));
    }

    public AiConnectionTestResponse testConnection(UpdateAiSettingsRequest request) {
        AiSettings settings = getOrCreateSettings();
        String baseUrl = request == null ? settings.getBaseUrl() : request.baseUrl();
        String model = request == null ? settings.getModel() : request.model();
        String apiKey = resolveApiKey(settings, request);

        if (!StringUtils.hasText(baseUrl) || !StringUtils.hasText(apiKey) || !StringUtils.hasText(model)) {
            return new AiConnectionTestResponse(false, "AI 配置不完整，请先填写 Base URL、API Key 和模型名称。");
        }

        aiProviderClient.chat(new AiChatRequest(
                LOCAL_USER_ID,
                baseUrl,
                apiKey,
                model,
                List.of(new AiMessage("user", "Return the word OK only.")),
                0.0,
                64,
                "text"
        ));

        return new AiConnectionTestResponse(true, "AI 模型连接成功。");
    }

    private AiSettings getOrCreateSettings() {
        return aiSettingsRepository.findByUserId(LOCAL_USER_ID)
                .orElseGet(() -> aiSettingsRepository.save(new AiSettings(LOCAL_USER_ID)));
    }

    private AiSettingsResponse toResponse(AiSettings settings) {
        return new AiSettingsResponse(
                settings.getProvider(),
                settings.getBaseUrl(),
                StringUtils.hasText(settings.getEncryptedApiKey()),
                settings.getApiKeyMask() == null ? "" : settings.getApiKeyMask(),
                settings.getModel(),
                settings.getTemperature(),
                settings.getMaxTokens()
        );
    }

    private boolean isConfigured(AiSettings settings) {
        return StringUtils.hasText(settings.getBaseUrl())
                && StringUtils.hasText(settings.getEncryptedApiKey())
                && StringUtils.hasText(settings.getModel());
    }

    private String resolveApiKey(AiSettings settings, UpdateAiSettingsRequest request) {
        if (request != null && StringUtils.hasText(request.apiKey())) {
            return request.apiKey();
        }

        if (!StringUtils.hasText(settings.getEncryptedApiKey())) {
            return "";
        }

        return secretCryptoService.decrypt(settings.getEncryptedApiKey());
    }

    private String maskApiKey(String value) {
        if (!StringUtils.hasText(value)) {
            return "";
        }

        if (value.length() <= 8) {
            return "****";
        }

        return value.substring(0, 4) + "..." + value.substring(value.length() - 4);
    }
}
