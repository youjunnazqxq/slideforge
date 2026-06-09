package com.slideforge.api.ai.provider;

public record AiChatResponse(
        String content,
        Integer promptTokens,
        Integer completionTokens,
        String rawResponse
) {
}
