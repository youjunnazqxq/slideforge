package com.slideforge.api.deck.dto;

import java.util.List;

public record SlideStickyNote(
        String slideId,
        Integer order,
        String sectionTitle,
        String title,
        String message,
        String status,
        List<String> tags
) {
}
