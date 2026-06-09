package com.slideforge.api.onepage;

import static org.assertj.core.api.Assertions.assertThat;

import com.slideforge.api.onepage.dto.CreateOnePageDraftResponse;
import com.slideforge.api.onepage.dto.PagePlan;
import com.slideforge.api.onepage.dto.VisualSpec;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

@SpringBootTest
class OnePageDraftServiceTest {

    @Autowired
    private OnePageDraftService onePageDraftService;

    @Test
    void updateVisualSpecFallsBackWhenCardsOverlap() {
        CreateOnePageDraftResponse created = onePageDraftService.createDraft("Build a one-page Bento slide");
        onePageDraftService.updatePagePlan(created.draftId(), pagePlan());

        VisualSpec normalized = onePageDraftService.updateVisualSpec(created.draftId(), overlappingVisualSpec());

        assertThat(normalized.canvas().width()).isEqualTo(1280);
        assertThat(normalized.canvas().height()).isEqualTo(720);
        assertThat(normalized.cards()).hasSize(4);
        assertThat(normalized.cards())
                .extracting(VisualSpec.Card::id)
                .containsExactly("hero", "byok", "risk", "next");
    }

    private PagePlan pagePlan() {
        return new PagePlan(
                "Bento slide",
                "Use a staged workflow.",
                "Keep the workflow controllable.",
                List.of(new PagePlan.ContentBlock("primary", "primary", "conclusion", "Workflow", "Stage the work.")),
                "Explain why staged generation reduces risk.",
                "Bento grid",
                "Professional"
        );
    }

    private VisualSpec overlappingVisualSpec() {
        return new VisualSpec(
                new VisualSpec.Canvas(1280, 720, "0 0 1280 720"),
                new VisualSpec.Theme("#F7F8FA", "#2563EB", "#111827", "#6B7280", "#FFFFFF", "#E5E7EB"),
                "mosaic",
                List.of(
                        new VisualSpec.Card("one", "primary", 64, 96, 500, 300, "primary"),
                        new VisualSpec.Card("two", "support", 80, 112, 420, 240, "secondary"),
                        new VisualSpec.Card("three", "next", 620, 96, 500, 300, "secondary")
                )
        );
    }
}
