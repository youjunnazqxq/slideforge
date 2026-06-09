package com.slideforge.api.deck.dto;

import java.util.List;

public record DeckDraftResponse(
        String deckId,
        String status,
        String initialPrompt,
        DeckOutline outline,
        List<SlideStickyNote> stickyNotes
) {
}
