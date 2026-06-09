package com.slideforge.api.user.dto;

import jakarta.validation.constraints.NotBlank;

public record UpdateUserProfileRequest(
        @NotBlank(message = "昵称不能为空")
        String nickname,

        @NotBlank(message = "默认语言不能为空")
        String defaultLanguage,

        @NotBlank(message = "默认 PPT 风格不能为空")
        String defaultPptStyle,

        @NotBlank(message = "默认画布比例不能为空")
        String defaultCanvasRatio
) {
}
