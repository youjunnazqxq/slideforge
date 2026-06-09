package com.slideforge.api.onepage.dto;

import jakarta.validation.constraints.NotBlank;

public record UpdateSvgRequest(
        @NotBlank String svgContent
) {
}
