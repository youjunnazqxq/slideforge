package com.slideforge.api.auth.dto;

public record LoginResponse(
        String username,
        String token
) {
}
