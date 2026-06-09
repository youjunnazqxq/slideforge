package com.slideforge.api.deck.dto;

public record DeckSlideDraftResponse(
        String slideId,
        int order,
        String title,
        String draftId,
        String status,
        String errorMessage
) {
}
