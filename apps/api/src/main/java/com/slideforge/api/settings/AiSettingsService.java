package com.slideforge.api.settings;

import com.slideforge.api.ai.provider.AiChatRequest;
import com.slideforge.api.ai.provider.AiMessage;
import com.slideforge.api.ai.provider.AiProviderClient;
import com.slideforge.api.settings.dto.AiConnectionTestResponse;
import com.slideforge.api.settings.dto.AiSettingsResponse;
import com.slideforge.api.settings.dto.UpdateAiSettingsRequest;
import java.util.List;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

@Service
public class AiSettingsService {

    private final AiProviderClient aiProviderClient;

    private String provider = "openai-compatible";
    private String baseUrl = "";
    private String apiKey = "";
    private String model = "";
    private double temperature = 0.7;
    private int maxTokens = 4096;

    public AiSettingsService(AiProviderClient aiProviderClient) {
        this.aiProviderClient = aiProviderClient;
    }

    public AiSettingsResponse getSettings() {
        return toResponse();
    }

    public AiSettingsResponse updateSettings(UpdateAiSettingsRequest request) {
        provider = request.provider();
        baseUrl = request.baseUrl();
        model = request.model();
        temperature = request.temperature() == null ? temperature : request.temperature();
        maxTokens = request.maxTokens() == null ? maxTokens : request.maxTokens();

        if (StringUtils.hasText(request.apiKey())) {
            apiKey = request.apiKey();
        }

        return toResponse();
    }

    public AiSettingsResponse deleteApiKey() {
        apiKey = "";
        return toResponse();
    }

    public AiConnectionTestResponse testConnection() {
        if (!isConfigured()) {
            return new AiConnectionTestResponse(false, "AI 配置不完整，请先填写 Base URL、API Key 和模型名称。");
        }

        aiProviderClient.chat(new AiChatRequest(
                "local-user",
                model,
                List.of(new AiMessage("user", "Return the word OK only.")),
                0.0,
                64,
                "text"
        ));

        return new AiConnectionTestResponse(true, "AI Provider Adapter 调用成功；真实模型请求可在该适配器中接入。");
    }

    private AiSettingsResponse toResponse() {
        return new AiSettingsResponse(
                provider,
                baseUrl,
                StringUtils.hasText(apiKey),
                maskApiKey(apiKey),
                model,
                temperature,
                maxTokens
        );
    }

    private boolean isConfigured() {
        return StringUtils.hasText(baseUrl) && StringUtils.hasText(apiKey) && StringUtils.hasText(model);
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
