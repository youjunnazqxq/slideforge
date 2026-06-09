package com.slideforge.api.settings.dto;

public record AiSettingsResponse(
        String provider,
        String baseUrl,
        boolean apiKeyConfigured,
        String apiKeyMask,
        String model,
        Double temperature,
        Integer maxTokens
) {
}
