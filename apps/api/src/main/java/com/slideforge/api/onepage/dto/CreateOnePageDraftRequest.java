package com.slideforge.api.onepage.dto;

import jakarta.validation.constraints.NotBlank;

public record CreateOnePageDraftRequest(
        @NotBlank(message = "初始需求不能为空")
        String initialPrompt
) {
}
