package com.slideforge.api.deck.dto;

import jakarta.validation.constraints.NotBlank;

public record CreateDeckDraftRequest(
        @NotBlank(message = "初始需求不能为空")
        String initialPrompt
) {
}
