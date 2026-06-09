package com.slideforge.api.deck;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.time.LocalDateTime;
import java.util.UUID;

@Entity
@Table(name = "deck_drafts")
public class DeckDraftEntity {

    @Id
    private UUID id;

    @Column(name = "user_id", nullable = false, length = 64)
    private String userId;

    @Column(name = "initial_prompt", nullable = false)
    private String initialPrompt;

    @Column(name = "outline_json")
    private String outlineJson;

    @Column(name = "research_pack_json")
    private String researchPackJson;

    @Column(name = "sticky_notes_json")
    private String stickyNotesJson;

    @Column(name = "generated_drafts_json")
    private String generatedDraftsJson;

    @Column(nullable = false, length = 64)
    private String status;

    @Column(name = "created_at", nullable = false)
    private LocalDateTime createdAt;

    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt;

    protected DeckDraftEntity() {
    }

    public DeckDraftEntity(String userId, String initialPrompt) {
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

    public String getOutlineJson() {
        return outlineJson;
    }

    public void setOutlineJson(String outlineJson) {
        this.outlineJson = outlineJson;
        touch();
    }

    public String getResearchPackJson() {
        return researchPackJson;
    }

    public void setResearchPackJson(String researchPackJson) {
        this.researchPackJson = researchPackJson;
        touch();
    }

    public String getStickyNotesJson() {
        return stickyNotesJson;
    }

    public void setStickyNotesJson(String stickyNotesJson) {
        this.stickyNotesJson = stickyNotesJson;
        touch();
    }

    public String getGeneratedDraftsJson() {
        return generatedDraftsJson;
    }

    public void setGeneratedDraftsJson(String generatedDraftsJson) {
        this.generatedDraftsJson = generatedDraftsJson;
        touch();
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
        touch();
    }

    private void touch() {
        this.updatedAt = LocalDateTime.now();
    }
}
