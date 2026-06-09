package com.slideforge.api.ai.provider;

import java.util.List;

public record AiChatRequest(
        String userId,
        String baseUrl,
        String apiKey,
        String model,
        List<AiMessage> messages,
        Double temperature,
        Integer maxTokens,
        String responseFormat
) {
}
