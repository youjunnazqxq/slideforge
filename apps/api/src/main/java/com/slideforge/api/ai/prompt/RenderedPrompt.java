package com.slideforge.api.ai.prompt;

import com.slideforge.api.ai.provider.AiMessage;
import java.util.List;

public record RenderedPrompt(
        String key,
        String version,
        List<AiMessage> messages,
        String responseFormat,
        Integer maxTokens,
        String renderedUserPrompt
) {
}
