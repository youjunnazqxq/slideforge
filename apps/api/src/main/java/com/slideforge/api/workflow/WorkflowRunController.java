package com.slideforge.api.workflow;

import com.slideforge.api.common.response.ApiResponse;
import com.slideforge.api.workflow.dto.WorkflowRunResponse;
import java.util.List;
import java.util.UUID;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/workflow-runs")
public class WorkflowRunController {

    private final WorkflowRunRepository workflowRunRepository;

    public WorkflowRunController(WorkflowRunRepository workflowRunRepository) {
        this.workflowRunRepository = workflowRunRepository;
    }

    @GetMapping
    public ApiResponse<List<WorkflowRunResponse>> listByDraft(@RequestParam String draftId) {
        return ApiResponse.success(workflowRunRepository.findByDraftIdOrderByCreatedAtDesc(UUID.fromString(draftId))
                .stream()
                .map(this::toResponse)
                .toList());
    }

    private WorkflowRunResponse toResponse(WorkflowRun run) {
        return new WorkflowRunResponse(
                run.getId().toString(),
                run.getDraftId() == null ? "" : run.getDraftId().toString(),
                run.getStage(),
                run.getModel(),
                run.getPromptKey(),
                run.getPromptVersion(),
                run.getStatus(),
                run.getErrorMessage(),
                run.getDurationMs(),
                run.getCreatedAt(),
                preview(run.getInputJson()),
                preview(run.getOutputJson())
        );
    }

    private String preview(String value) {
        if (value == null) {
            return "";
        }

        String compact = value.replaceAll("\\s+", " ").trim();
        return compact.length() <= 320 ? compact : compact.substring(0, 320) + "...";
    }
}
