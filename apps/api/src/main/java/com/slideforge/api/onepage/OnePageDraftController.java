package com.slideforge.api.onepage;

import com.slideforge.api.common.response.ApiResponse;
import com.slideforge.api.onepage.dto.ConsultRequest;
import com.slideforge.api.onepage.dto.ConsultResponse;
import com.slideforge.api.onepage.dto.CreateOnePageDraftRequest;
import com.slideforge.api.onepage.dto.CreateOnePageDraftResponse;
import com.slideforge.api.onepage.dto.GenerateResearchRequest;
import com.slideforge.api.onepage.dto.OnePageDraftResponse;
import com.slideforge.api.onepage.dto.PagePlan;
import com.slideforge.api.onepage.dto.RequirementBrief;
import com.slideforge.api.onepage.dto.ResearchPack;
import com.slideforge.api.onepage.dto.SvgGenerateResponse;
import jakarta.validation.Valid;
import org.springframework.http.ContentDisposition;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/one-page/drafts")
public class OnePageDraftController {

    private final OnePageDraftService onePageDraftService;
    private final OnePagePptxExportService onePagePptxExportService;

    public OnePageDraftController(
            OnePageDraftService onePageDraftService,
            OnePagePptxExportService onePagePptxExportService
    ) {
        this.onePageDraftService = onePageDraftService;
        this.onePagePptxExportService = onePagePptxExportService;
    }

    @PostMapping
    public ApiResponse<CreateOnePageDraftResponse> createDraft(
            @Valid @RequestBody CreateOnePageDraftRequest request
    ) {
        return ApiResponse.success(onePageDraftService.createDraft(request.initialPrompt()));
    }

    @GetMapping("/{draftId}")
    public ApiResponse<OnePageDraftResponse> getDraft(@PathVariable String draftId) {
        return ApiResponse.success(onePageDraftService.getDraft(draftId));
    }

    @PostMapping("/{draftId}/consult")
    public ApiResponse<ConsultResponse> consult(
            @PathVariable String draftId,
            @Valid @RequestBody ConsultRequest request
    ) {
        return ApiResponse.success(onePageDraftService.consult(draftId, request.message()));
    }

    @PostMapping("/{draftId}/brief")
    public ApiResponse<RequirementBrief> generateBrief(@PathVariable String draftId) {
        return ApiResponse.success(onePageDraftService.generateBrief(draftId));
    }

    @PutMapping("/{draftId}/brief")
    public ApiResponse<RequirementBrief> updateBrief(
            @PathVariable String draftId,
            @RequestBody RequirementBrief brief
    ) {
        return ApiResponse.success(onePageDraftService.updateBrief(draftId, brief));
    }

    @PostMapping("/{draftId}/research")
    public ApiResponse<ResearchPack> generateResearch(
            @PathVariable String draftId,
            @RequestBody(required = false) GenerateResearchRequest request
    ) {
        String mode = request == null ? "model-only" : request.normalizedMode();
        return ApiResponse.success(onePageDraftService.generateResearch(draftId, mode));
    }

    @PostMapping("/{draftId}/page-plan")
    public ApiResponse<PagePlan> generatePagePlan(@PathVariable String draftId) {
        return ApiResponse.success(onePageDraftService.generatePagePlan(draftId));
    }

    @PutMapping("/{draftId}/page-plan")
    public ApiResponse<PagePlan> updatePagePlan(
            @PathVariable String draftId,
            @RequestBody PagePlan pagePlan
    ) {
        return ApiResponse.success(onePageDraftService.updatePagePlan(draftId, pagePlan));
    }

    @PostMapping("/{draftId}/svg")
    public ApiResponse<SvgGenerateResponse> generateSvg(@PathVariable String draftId) {
        return ApiResponse.success(onePageDraftService.generateSvg(draftId));
    }

    @PostMapping("/{draftId}/svg/regenerate")
    public ApiResponse<SvgGenerateResponse> regenerateSvg(@PathVariable String draftId) {
        return ApiResponse.success(onePageDraftService.generateSvg(draftId));
    }

    @PostMapping("/{draftId}/export/pptx")
    public ResponseEntity<byte[]> exportPptx(@PathVariable String draftId) {
        OnePagePptxExportService.ExportedPptx exported = onePagePptxExportService.exportDraft(draftId);

        return ResponseEntity.ok()
                .contentType(MediaType.parseMediaType("application/vnd.openxmlformats-officedocument.presentationml.presentation"))
                .header(
                        HttpHeaders.CONTENT_DISPOSITION,
                        ContentDisposition.attachment().filename(exported.fileName()).build().toString()
                )
                .body(exported.content());
    }
}
