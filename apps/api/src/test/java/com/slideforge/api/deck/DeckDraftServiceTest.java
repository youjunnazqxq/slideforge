package com.slideforge.api.deck;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.atLeast;
import static org.mockito.Mockito.when;

import com.slideforge.api.ai.AiRuntimeService;
import com.slideforge.api.ai.provider.AiChatResponse;
import com.slideforge.api.deck.dto.CreateDeckDraftResponse;
import com.slideforge.api.deck.dto.DeckDraftResponse;
import com.slideforge.api.onepage.OnePageDraftService;
import com.slideforge.api.onepage.dto.CreateOnePageDraftResponse;
import com.slideforge.api.onepage.dto.PagePlan;
import com.slideforge.api.onepage.dto.SvgGenerateResponse;
import com.slideforge.api.onepage.dto.ValidationReport;
import com.slideforge.api.onepage.dto.VisualSpec;
import com.slideforge.api.research.SearchClient;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;

@SpringBootTest
class DeckDraftServiceTest {

    @Autowired
    private DeckDraftService deckDraftService;

    @MockBean
    private AiRuntimeService aiRuntimeService;

    @MockBean
    private OnePageDraftService onePageDraftService;

    @MockBean
    private SearchClient searchClient;

    @Test
    void runAgentFlowGeneratesSvgReadyDrafts() {
        when(searchClient.available()).thenReturn(false);
        when(aiRuntimeService.chat(anyString(), anyList(), anyString(), any())).thenReturn(
                new AiChatResponse("需求足够，可以生成 brief。", 1, 1, "{}"),
                new AiChatResponse(researchJson(), 1, 1, "{}"),
                new AiChatResponse(outlineJson(), 1, 1, "{}")
        );
        when(onePageDraftService.createDraft(anyString())).thenAnswer(invocation ->
                new CreateOnePageDraftResponse(UUID.randomUUID().toString(), "CREATED"));
        when(onePageDraftService.generatePagePlan(anyString())).thenReturn(pagePlan());
        when(onePageDraftService.generateVisualSpec(anyString())).thenReturn(visualSpec());
        when(onePageDraftService.generateSvg(anyString())).thenReturn(new SvgGenerateResponse(
                "<svg width=\"1280\" height=\"720\" viewBox=\"0 0 1280 720\"></svg>",
                new ValidationReport(true, List.of())
        ));

        CreateDeckDraftResponse created = deckDraftService.createDraft("AI PPT Agent 内部立项汇报");
        DeckDraftResponse result = deckDraftService.runAgentFlow(created.deckId(), "model-only");

        assertThat(result.status()).isEqualTo("SLIDE_SVGS_READY");
        assertThat(result.stickyNotes()).hasSize(5);
        assertThat(result.generatedDrafts()).hasSize(5);
        assertThat(result.generatedDrafts()).allMatch(draft -> "SVG_READY".equals(draft.status()));
        Mockito.verify(onePageDraftService, atLeast(5)).generatePagePlan(anyString());
        Mockito.verify(onePageDraftService, atLeast(5)).generateVisualSpec(anyString());
        Mockito.verify(onePageDraftService, atLeast(5)).generateSvg(anyString());
    }

    private String researchJson() {
        return """
                {
                  "mode": "model-only",
                  "summary": "AI PPT Agent should use a staged workflow.",
                  "keyPoints": ["consult first", "plan pages", "validate SVG"],
                  "evidence": [],
                  "sources": [],
                  "limitations": []
                }
                """;
    }

    private String outlineJson() {
        return """
                {
                  "title": "AI PPT Agent Project",
                  "audience": "Internal team",
                  "scenario": "Project kickoff",
                  "coreThesis": "Build a controllable staged PPT agent.",
                  "structure": [
                    {"id": "section-1", "title": "Why now", "purpose": "Frame the opportunity"}
                  ],
                  "slides": [
                    {"id": "slide-001", "type": "cover", "sectionId": "section-1", "title": "AI PPT Agent Project", "message": "A controllable staged workflow.", "purpose": "Open"},
                    {"id": "slide-002", "type": "agenda", "sectionId": "section-1", "title": "Agenda", "message": "Opportunity / MVP / next steps", "purpose": "Navigate"},
                    {"id": "slide-003", "type": "section", "sectionId": "section-1", "title": "Why now", "message": "The workflow is ready to productize.", "purpose": "Transition"},
                    {"id": "slide-004", "type": "content", "sectionId": "section-1", "title": "MVP Loop", "message": "Validate with one-page drafts before scaling.", "purpose": "Argue"},
                    {"id": "slide-005", "type": "summary", "sectionId": "section-1", "title": "Next steps", "message": "Ship the staged MVP first.", "purpose": "Close"}
                  ]
                }
                """;
    }

    private PagePlan pagePlan() {
        return new PagePlan(
                "MVP Loop",
                "Validate the staged workflow.",
                "Start with a controllable loop.",
                List.of(new PagePlan.ContentBlock("primary", "primary", "conclusion", "MVP", "Ship the loop.")),
                "Show why the MVP loop is the first proof point.",
                "Bento grid",
                "Professional"
        );
    }

    private VisualSpec visualSpec() {
        return new VisualSpec(
                new VisualSpec.Canvas(1280, 720, "0 0 1280 720"),
                new VisualSpec.Theme("#F7F8FA", "#2563EB", "#111827", "#6B7280", "#FFFFFF", "#E5E7EB"),
                "hero-left",
                List.of(new VisualSpec.Card("hero", "primary", 64, 96, 560, 520, "primary"))
        );
    }
}
