package com.slideforge.api.deck;

import com.slideforge.api.common.response.ApiResponse;
import com.slideforge.api.deck.dto.CreateDeckDraftRequest;
import com.slideforge.api.deck.dto.CreateDeckDraftResponse;
import com.slideforge.api.deck.dto.DeckDraftResponse;
import com.slideforge.api.deck.dto.DeckOutline;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
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

    @PostMapping("/{deckId}/outline")
    public ApiResponse<DeckOutline> generateOutline(@PathVariable String deckId) {
        return ApiResponse.success(deckDraftService.generateOutline(deckId));
    }
}
