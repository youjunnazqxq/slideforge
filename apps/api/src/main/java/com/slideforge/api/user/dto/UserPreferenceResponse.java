package com.slideforge.api.user.dto;

public record UserPreferenceResponse(
        String defaultLanguage,
        String defaultPptStyle,
        String defaultCanvasRatio
) {
}
