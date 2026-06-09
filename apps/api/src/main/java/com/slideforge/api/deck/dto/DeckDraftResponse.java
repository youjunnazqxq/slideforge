package com.slideforge.api.deck.dto;

import com.slideforge.api.onepage.dto.ResearchPack;
import java.util.List;

public record DeckDraftResponse(
        String deckId,
        String status,
        String initialPrompt,
        ResearchPack researchPack,
        DeckOutline outline,
        List<SlideStickyNote> stickyNotes,
        List<DeckSlideDraftResponse> generatedDrafts
) {
}
