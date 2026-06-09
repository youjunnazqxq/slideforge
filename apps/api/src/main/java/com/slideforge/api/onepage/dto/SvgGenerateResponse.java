package com.slideforge.api.onepage.dto;

public record SvgGenerateResponse(
        String svgContent,
        ValidationReport validationReport
) {
}
