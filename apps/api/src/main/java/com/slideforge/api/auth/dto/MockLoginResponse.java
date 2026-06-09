package com.slideforge.api.auth.dto;

public record MockLoginResponse(
        String accessToken,
        AuthUserResponse user
) {
}
