package com.slideforge.api.onepage;

import com.slideforge.api.ai.prompt.PromptKeys;
import com.slideforge.api.ai.prompt.PromptRenderer;
import com.slideforge.api.ai.prompt.PromptTemplateRegistry;
import com.slideforge.api.ai.prompt.RenderedPrompt;
import java.util.Map;
import org.springframework.stereotype.Service;

@Service
public class OnePagePromptService {

    private final PromptTemplateRegistry promptTemplateRegistry;
    private final PromptRenderer promptRenderer;

    public OnePagePromptService(
            PromptTemplateRegistry promptTemplateRegistry,
            PromptRenderer promptRenderer
    ) {
        this.promptTemplateRegistry = promptTemplateRegistry;
        this.promptRenderer = promptRenderer;
    }

    public RenderedPrompt consultant(String initialPrompt, String message) {
        return render(PromptKeys.CONSULTANT, Map.of(
                "initialPrompt", initialPrompt,
                "message", message
        ));
    }

    public RenderedPrompt brief(String initialPrompt) {
        return render(PromptKeys.BRIEF_EXTRACT, Map.of("initialPrompt", initialPrompt));
    }

    public RenderedPrompt research(String requirementBriefJson, String researchMode, String sourcesJson) {
        return render(PromptKeys.RESEARCH_COLLECT, Map.of(
                "requirementBriefJson", requirementBriefJson,
                "researchMode", researchMode,
                "sourcesJson", sourcesJson
        ));
    }

    public RenderedPrompt pagePlan(String requirementBriefJson, String researchPackJson) {
        return render(PromptKeys.PAGE_PLAN_GENERATE, Map.of(
                "requirementBriefJson", requirementBriefJson,
                "researchPackJson", researchPackJson
        ));
    }

    public RenderedPrompt svg(String pagePlanJson) {
        return render(PromptKeys.SVG_GENERATE, Map.of("pagePlanJson", pagePlanJson));
    }

    private RenderedPrompt render(String key, Map<String, String> variables) {
        return promptRenderer.render(promptTemplateRegistry.get(key), variables);
    }
}
