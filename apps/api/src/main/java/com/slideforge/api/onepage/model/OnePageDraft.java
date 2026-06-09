package com.slideforge.api.onepage.model;

import com.slideforge.api.onepage.dto.PagePlan;
import com.slideforge.api.onepage.dto.RequirementBrief;
import com.slideforge.api.onepage.dto.ResearchPack;
import com.slideforge.api.onepage.dto.ValidationReport;
import com.slideforge.api.onepage.dto.VisualSpec;

public class OnePageDraft {

    private final String id;
    private final String initialPrompt;
    private String status;
    private RequirementBrief requirementBrief;
    private ResearchPack researchPack;
    private PagePlan pagePlan;
    private VisualSpec visualSpec;
    private String svgContent;
    private ValidationReport validationReport;

    public OnePageDraft(String id, String initialPrompt) {
        this.id = id;
        this.initialPrompt = initialPrompt;
        this.status = "CREATED";
    }

    public String getId() {
        return id;
    }

    public String getInitialPrompt() {
        return initialPrompt;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public RequirementBrief getRequirementBrief() {
        return requirementBrief;
    }

    public void setRequirementBrief(RequirementBrief requirementBrief) {
        this.requirementBrief = requirementBrief;
    }

    public ResearchPack getResearchPack() {
        return researchPack;
    }

    public void setResearchPack(ResearchPack researchPack) {
        this.researchPack = researchPack;
    }

    public PagePlan getPagePlan() {
        return pagePlan;
    }

    public void setPagePlan(PagePlan pagePlan) {
        this.pagePlan = pagePlan;
    }

    public VisualSpec getVisualSpec() {
        return visualSpec;
    }

    public void setVisualSpec(VisualSpec visualSpec) {
        this.visualSpec = visualSpec;
    }

    public String getSvgContent() {
        return svgContent;
    }

    public void setSvgContent(String svgContent) {
        this.svgContent = svgContent;
    }

    public ValidationReport getValidationReport() {
        return validationReport;
    }

    public void setValidationReport(ValidationReport validationReport) {
        this.validationReport = validationReport;
    }
}
