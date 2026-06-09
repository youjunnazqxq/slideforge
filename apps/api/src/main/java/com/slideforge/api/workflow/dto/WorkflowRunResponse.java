package com.slideforge.api.workflow.dto;

import java.time.LocalDateTime;

public record WorkflowRunResponse(
        String id,
        String draftId,
        String stage,
        String model,
        String promptKey,
        String promptVersion,
        String status,
        String errorMessage,
        Integer durationMs,
        LocalDateTime createdAt,
        String inputPreview,
        String outputPreview
) {
}
