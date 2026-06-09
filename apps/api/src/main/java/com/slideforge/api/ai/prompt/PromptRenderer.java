package com.slideforge.api.ai.prompt;

import com.slideforge.api.ai.provider.AiMessage;
import java.util.List;
import java.util.Map;
import org.springframework.stereotype.Component;

@Component
public class PromptRenderer {

    public RenderedPrompt render(PromptTemplate template, Map<String, String> variables) {
        String userPrompt = template.userTemplate();

        for (Map.Entry<String, String> entry : variables.entrySet()) {
            userPrompt = userPrompt.replace("{{" + entry.getKey() + "}}", entry.getValue() == null ? "" : entry.getValue());
        }

        return new RenderedPrompt(
                template.key(),
                template.version(),
                List.of(
                        new AiMessage("system", template.systemPrompt()),
                        new AiMessage("user", userPrompt)
                ),
                template.responseFormat(),
                template.maxTokens(),
                userPrompt
        );
    }
}
