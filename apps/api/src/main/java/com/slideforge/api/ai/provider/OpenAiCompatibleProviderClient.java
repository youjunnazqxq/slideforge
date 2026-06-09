package com.slideforge.api.ai.provider;

import org.springframework.stereotype.Component;

@Component
public class OpenAiCompatibleProviderClient implements AiProviderClient {

    @Override
    public String providerName() {
        return "openai-compatible";
    }

    @Override
    public AiChatResponse chat(AiChatRequest request) {
        String content = "OK";
        return new AiChatResponse(content, null, null, content);
    }
}
