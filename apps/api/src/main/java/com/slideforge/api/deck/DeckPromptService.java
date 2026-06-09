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
                Map.of("initialPrompt", withDeckSkeletonRequirements(initialPrompt))
        );
    }

    private String withDeckSkeletonRequirements(String initialPrompt) {
        return """
                %s

                Required deck skeleton:
                - The first slide must be type="cover".
                - The second slide must be type="agenda".
                - Add one type="section" divider before each major chapter.
                - Add multiple type="content" slides after the matching section divider.
                - The last slide must be type="summary".
                - Keep every slide as one clear communication task, suitable for sticky-note editing and one-page SVG generation.
                """.formatted(initialPrompt == null ? "" : initialPrompt);
    }
}
