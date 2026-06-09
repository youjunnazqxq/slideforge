package com.slideforge.api.onepage.dto;

public record OnePageDraftResponse(
        String draftId,
        String status,
        String initialPrompt,
        RequirementBrief requirementBrief,
        ResearchPack researchPack,
        PagePlan pagePlan,
        VisualSpec visualSpec,
        String svgContent,
        ValidationReport validationReport
) {
}
