package com.slideforge.api.deck;

import com.slideforge.api.common.response.ApiResponse;
import com.slideforge.api.deck.dto.CreateDeckDraftRequest;
import com.slideforge.api.deck.dto.CreateDeckDraftResponse;
import com.slideforge.api.deck.dto.DeckDraftResponse;
import com.slideforge.api.deck.dto.DeckOutline;
import com.slideforge.api.deck.dto.DeckSlideDraftResponse;
import com.slideforge.api.deck.dto.SlideStickyNote;
import com.slideforge.api.onepage.dto.ConsultRequest;
import com.slideforge.api.onepage.dto.ConsultResponse;
import com.slideforge.api.onepage.dto.CreateOnePageDraftResponse;
import com.slideforge.api.onepage.dto.GenerateResearchRequest;
import com.slideforge.api.onepage.dto.ResearchPack;
import jakarta.validation.Valid;
import java.util.List;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/decks")
public class DeckDraftController {

    private final DeckDraftService deckDraftService;

    public DeckDraftController(DeckDraftService deckDraftService) {
        this.deckDraftService = deckDraftService;
    }

    @PostMapping
    public ApiResponse<CreateDeckDraftResponse> createDraft(
            @Valid @RequestBody CreateDeckDraftRequest request
    ) {
        return ApiResponse.success(deckDraftService.createDraft(request.initialPrompt()));
    }

    @GetMapping("/{deckId}")
    public ApiResponse<DeckDraftResponse> getDraft(@PathVariable String deckId) {
        return ApiResponse.success(deckDraftService.getDraft(deckId));
    }

    @PostMapping("/{deckId}/consult")
    public ApiResponse<ConsultResponse> consult(
            @PathVariable String deckId,
            @Valid @RequestBody ConsultRequest request
    ) {
        return ApiResponse.success(deckDraftService.consult(deckId, request.message()));
    }

    @PostMapping("/{deckId}/outline")
    public ApiResponse<DeckOutline> generateOutline(@PathVariable String deckId) {
        return ApiResponse.success(deckDraftService.generateOutline(deckId));
    }

    @PostMapping("/{deckId}/research")
    public ApiResponse<ResearchPack> generateResearch(
            @PathVariable String deckId,
            @RequestBody(required = false) GenerateResearchRequest request
    ) {
        String mode = request == null ? "model-only" : request.normalizedMode();
        return ApiResponse.success(deckDraftService.generateResearch(deckId, mode));
    }

    @PutMapping("/{deckId}/sticky-notes")
    public ApiResponse<List<SlideStickyNote>> saveStickyNotes(
            @PathVariable String deckId,
            @RequestBody List<SlideStickyNote> stickyNotes
    ) {
        return ApiResponse.success(deckDraftService.saveStickyNotes(deckId, stickyNotes));
    }

    @PostMapping("/{deckId}/sticky-notes")
    public ApiResponse<SlideStickyNote> addStickyNote(
            @PathVariable String deckId,
            @RequestBody SlideStickyNote stickyNote
    ) {
        return ApiResponse.success(deckDraftService.addStickyNote(deckId, stickyNote));
    }

    @DeleteMapping("/{deckId}/sticky-notes/{slideId}")
    public ApiResponse<List<SlideStickyNote>> deleteStickyNote(
            @PathVariable String deckId,
            @PathVariable String slideId
    ) {
        return ApiResponse.success(deckDraftService.deleteStickyNote(deckId, slideId));
    }

    @PostMapping("/{deckId}/slides/{slideId}/one-page-draft")
    public ApiResponse<CreateOnePageDraftResponse> createOnePageDraftFromSlide(
            @PathVariable String deckId,
            @PathVariable String slideId
    ) {
        return ApiResponse.success(deckDraftService.createOnePageDraftFromSlide(deckId, slideId));
    }

    @PostMapping("/{deckId}/slides/{slideId}/svg-draft")
    public ApiResponse<DeckSlideDraftResponse> createSvgDraftFromSlide(
            @PathVariable String deckId,
            @PathVariable String slideId
    ) {
        return ApiResponse.success(deckDraftService.createSvgDraftFromSlide(deckId, slideId));
    }

    @PostMapping("/{deckId}/slides/one-page-drafts")
    public ApiResponse<List<DeckSlideDraftResponse>> createOnePageDraftsFromSlides(
            @PathVariable String deckId
    ) {
        return ApiResponse.success(deckDraftService.createOnePageDraftsFromSlides(deckId));
    }

    @PostMapping("/{deckId}/slides/page-plan-drafts")
    public ApiResponse<List<DeckSlideDraftResponse>> createPagePlanDraftsFromSlides(
            @PathVariable String deckId
    ) {
        return ApiResponse.success(deckDraftService.createPagePlanDraftsFromSlides(deckId));
    }

    @PostMapping("/{deckId}/slides/visual-spec-drafts")
    public ApiResponse<List<DeckSlideDraftResponse>> createVisualSpecDraftsFromSlides(
            @PathVariable String deckId
    ) {
        return ApiResponse.success(deckDraftService.createVisualSpecDraftsFromSlides(deckId));
    }

    @PostMapping("/{deckId}/slides/svg-drafts")
    public ApiResponse<List<DeckSlideDraftResponse>> createSvgDraftsFromSlides(
            @PathVariable String deckId
    ) {
        return ApiResponse.success(deckDraftService.createSvgDraftsFromSlides(deckId));
    }
}
