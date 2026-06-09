package com.slideforge.api.onepage.dto;

import java.util.List;

public record RequirementBrief(
        String topic,
        String audience,
        String scenario,
        String goal,
        String coreConclusion,
        String tone,
        List<String> mustInclude,
        List<String> avoid,
        String language,
        String canvasRatio
) {
}
