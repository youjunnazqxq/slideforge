package com.slideforge.api.auth.dto;

public record AuthUserResponse(
        String id,
        String email,
        String nickname,
        String role
) {
}
