package com.slideforge.api.onepage;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.time.LocalDateTime;
import java.util.UUID;

@Entity
@Table(name = "one_page_drafts")
public class OnePageDraftEntity {

    @Id
    private UUID id;

    @Column(name = "user_id", nullable = false, length = 64)
    private String userId;

    @Column(name = "initial_prompt", nullable = false)
    private String initialPrompt;

    @Column(name = "conversation_json")
    private String conversationJson;

    @Column(name = "requirement_brief_json")
    private String requirementBriefJson;

    @Column(name = "research_pack_json")
    private String researchPackJson;

    @Column(name = "page_plan_json")
    private String pagePlanJson;

    @Column(name = "visual_spec_json")
    private String visualSpecJson;

    @Column(name = "svg_content")
    private String svgContent;

    @Column(name = "validation_report_json")
    private String validationReportJson;

    @Column(nullable = false, length = 64)
    private String status;

    @Column(name = "failed_stage", length = 64)
    private String failedStage;

    @Column(name = "error_message")
    private String errorMessage;

    @Column(name = "created_at", nullable = false)
    private LocalDateTime createdAt;

    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt;

    protected OnePageDraftEntity() {
    }

    public OnePageDraftEntity(String userId, String initialPrompt) {
        LocalDateTime now = LocalDateTime.now();
        this.id = UUID.randomUUID();
        this.userId = userId;
        this.initialPrompt = initialPrompt;
        this.status = "CREATED";
        this.createdAt = now;
        this.updatedAt = now;
    }

    public UUID getId() {
        return id;
    }

    public String getInitialPrompt() {
        return initialPrompt;
    }

    public String getRequirementBriefJson() {
        return requirementBriefJson;
    }

    public void setRequirementBriefJson(String requirementBriefJson) {
        this.requirementBriefJson = requirementBriefJson;
        touch();
    }

    public String getResearchPackJson() {
        return researchPackJson;
    }

    public void setResearchPackJson(String researchPackJson) {
        this.researchPackJson = researchPackJson;
        touch();
    }

    public String getPagePlanJson() {
        return pagePlanJson;
    }

    public void setPagePlanJson(String pagePlanJson) {
        this.pagePlanJson = pagePlanJson;
        touch();
    }

    public String getVisualSpecJson() {
        return visualSpecJson;
    }

    public void setVisualSpecJson(String visualSpecJson) {
        this.visualSpecJson = visualSpecJson;
        touch();
    }

    public String getSvgContent() {
        return svgContent;
    }

    public void setSvgContent(String svgContent) {
        this.svgContent = svgContent;
        touch();
    }

    public String getValidationReportJson() {
        return validationReportJson;
    }

    public void setValidationReportJson(String validationReportJson) {
        this.validationReportJson = validationReportJson;
        touch();
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
        touch();
    }

    public void setFailed(String failedStage, String errorMessage) {
        this.failedStage = failedStage;
        this.errorMessage = errorMessage;
        this.status = "FAILED";
        touch();
    }

    private void touch() {
        this.updatedAt = LocalDateTime.now();
    }
}

