package com.slideforge.api.ai;

import com.slideforge.api.ai.provider.AiChatRequest;
import com.slideforge.api.ai.provider.AiChatResponse;
import com.slideforge.api.ai.provider.AiMessage;
import com.slideforge.api.ai.provider.AiProviderClient;
import com.slideforge.api.security.SecretCryptoService;
import com.slideforge.api.settings.AiSettings;
import com.slideforge.api.settings.AiSettingsRepository;
import java.util.List;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;
import org.springframework.web.server.ResponseStatusException;

@Service
public class AiRuntimeService {

    private final AiSettingsRepository aiSettingsRepository;
    private final AiProviderClient aiProviderClient;
    private final SecretCryptoService secretCryptoService;

    public AiRuntimeService(
            AiSettingsRepository aiSettingsRepository,
            AiProviderClient aiProviderClient,
            SecretCryptoService secretCryptoService
    ) {
        this.aiSettingsRepository = aiSettingsRepository;
        this.aiProviderClient = aiProviderClient;
        this.secretCryptoService = secretCryptoService;
    }

    public AiChatResponse chat(String userId, List<AiMessage> messages, String responseFormat, Integer maxTokens) {
        AiSettings settings = aiSettingsRepository.findByUserId(userId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.BAD_REQUEST, "请先配置 AI API。"));

        if (!StringUtils.hasText(settings.getBaseUrl())
                || !StringUtils.hasText(settings.getEncryptedApiKey())
                || !StringUtils.hasText(settings.getModel())) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "AI 配置不完整，请先填写 Base URL、API Key 和模型名称。");
        }

        return aiProviderClient.chat(new AiChatRequest(
                userId,
                settings.getBaseUrl(),
                secretCryptoService.decrypt(settings.getEncryptedApiKey()),
                settings.getModel(),
                messages,
                settings.getTemperature(),
                maxTokens == null ? settings.getMaxTokens() : maxTokens,
                responseFormat
        ));
    }
}
