package com.slideforge.api.workflow;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.time.LocalDateTime;
import java.util.UUID;

@Entity
@Table(name = "workflow_runs")
public class WorkflowRun {

    @Id
    private UUID id;

    @Column(name = "user_id", nullable = false, length = 64)
    private String userId;

    @Column(name = "draft_id")
    private UUID draftId;

    @Column(nullable = false, length = 64)
    private String stage;

    @Column(length = 128)
    private String model;

    @Column(name = "prompt_key", length = 128)
    private String promptKey;

    @Column(name = "prompt_version", length = 32)
    private String promptVersion;

    @Column(name = "input_json")
    private String inputJson;

    @Column(name = "output_json")
    private String outputJson;

    @Column(nullable = false, length = 32)
    private String status;

    @Column(name = "error_message")
    private String errorMessage;

    @Column(name = "prompt_tokens")
    private Integer promptTokens;

    @Column(name = "completion_tokens")
    private Integer completionTokens;

    @Column(name = "duration_ms")
    private Integer durationMs;

    @Column(name = "created_at", nullable = false)
    private LocalDateTime createdAt;

    protected WorkflowRun() {
    }

    public WorkflowRun(
            String userId,
            UUID draftId,
            String stage,
            String model,
            String promptKey,
            String inputJson,
            String outputJson,
            String status,
            String errorMessage,
            long durationMs
    ) {
        this.id = UUID.randomUUID();
        this.userId = userId;
        this.draftId = draftId;
        this.stage = stage;
        this.model = model;
        this.promptKey = promptKey;
        this.promptVersion = "v1";
        this.inputJson = inputJson;
        this.outputJson = outputJson;
        this.status = status;
        this.errorMessage = errorMessage;
        this.durationMs = Math.toIntExact(Math.min(durationMs, Integer.MAX_VALUE));
        this.createdAt = LocalDateTime.now();
    }

    public UUID getId() {
        return id;
    }

    public UUID getDraftId() {
        return draftId;
    }

    public String getStage() {
        return stage;
    }

    public String getModel() {
        return model;
    }

    public String getPromptKey() {
        return promptKey;
    }

    public String getPromptVersion() {
        return promptVersion;
    }

    public String getInputJson() {
        return inputJson;
    }

    public String getOutputJson() {
        return outputJson;
    }

    public String getStatus() {
        return status;
    }

    public String getErrorMessage() {
        return errorMessage;
    }

    public Integer getDurationMs() {
        return durationMs;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }
}
