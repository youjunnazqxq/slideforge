package com.slideforge.api.user.dto;

public record UserProfileResponse(
        String id,
        String email,
        String nickname,
        String role,
        UserPreferenceResponse preferences
) {
}
