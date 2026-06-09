package com.slideforge.api.deck.dto;

import java.util.List;

public record DeckOutline(
        String title,
        String audience,
        String scenario,
        String coreThesis,
        List<Section> structure,
        List<Slide> slides
) {
    public record Section(String id, String title, String purpose) {
    }

    public record Slide(
            String id,
            String type,
            String sectionId,
            String title,
            String message,
            String purpose
    ) {
    }
}
