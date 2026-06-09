package com.slideforge.api.deck;

import com.slideforge.api.ai.prompt.PromptKeys;
import com.slideforge.api.ai.prompt.PromptRenderer;
import com.slideforge.api.ai.prompt.PromptTemplateRegistry;
import com.slideforge.api.ai.prompt.RenderedPrompt;
import java.util.Map;
import org.springframework.stereotype.Service;

@Service
public class DeckPromptService {

    private final PromptTemplateRegistry promptTemplateRegistry;
    private final PromptRenderer promptRenderer;

    public DeckPromptService(PromptTemplateRegistry promptTemplateRegistry, PromptRenderer promptRenderer) {
        this.promptTemplateRegistry = promptTemplateRegistry;
        this.promptRenderer = promptRenderer;
    }

    public RenderedPrompt outline(String initialPrompt) {
        return promptRenderer.render(
                promptTemplateRegistry.get(PromptKeys.DECK_OUTLINE),
                Map.of("initialPrompt", initialPrompt)
        );
    }
}
