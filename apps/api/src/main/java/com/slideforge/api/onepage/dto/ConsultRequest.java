package com.slideforge.api.onepage.dto;

import jakarta.validation.constraints.NotBlank;

public record ConsultRequest(
        @NotBlank(message = "消息不能为空")
        String message
) {
}
