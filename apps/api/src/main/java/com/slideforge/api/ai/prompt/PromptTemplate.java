package com.slideforge.api.ai.prompt;

public record PromptTemplate(
        String key,
        String version,
        String systemPrompt,
        String userTemplate,
        String responseFormat,
        Integer maxTokens
) {
}
