package com.slideforge.api.onepage.dto;

public record GenerateResearchRequest(
        String mode
) {
    public String normalizedMode() {
        if ("search-assisted".equalsIgnoreCase(mode)) {
            return "search-assisted";
        }

        return "model-only";
    }
}
