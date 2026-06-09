package com.slideforge.api.settings.dto;

import jakarta.validation.constraints.NotBlank;

public record UpdateAiSettingsRequest(
        @NotBlank(message = "服务商不能为空")
        String provider,

        @NotBlank(message = "Base URL 不能为空")
        String baseUrl,

        String apiKey,

        @NotBlank(message = "默认模型不能为空")
        String model,

        Double temperature,

        Integer maxTokens
) {
}
