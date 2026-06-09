package com.slideforge.api.onepage.dto;

import java.util.List;

public record VisualSpec(
        Canvas canvas,
        Theme theme,
        String layoutPattern,
        List<Card> cards
) {

    public record Canvas(int width, int height, String viewBox) {
    }

    public record Theme(
            String background,
            String primary,
            String text,
            String muted,
            String card,
            String border
    ) {
    }

    public record Card(
            String id,
            String blockId,
            int x,
            int y,
            int w,
            int h,
            String priority
    ) {
    }
}
