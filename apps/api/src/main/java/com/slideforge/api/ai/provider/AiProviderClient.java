package com.slideforge.api.ai.provider;

public interface AiProviderClient {

    String providerName();

    AiChatResponse chat(AiChatRequest request);
}
