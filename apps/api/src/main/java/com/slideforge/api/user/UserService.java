package com.slideforge.api.user;

import com.slideforge.api.user.dto.UpdateUserProfileRequest;
import com.slideforge.api.user.dto.UserPreferenceResponse;
import com.slideforge.api.user.dto.UserProfileResponse;
import org.springframework.stereotype.Service;

@Service
public class UserService {

    private UserProfileResponse currentUser = new UserProfileResponse(
            "local-user",
            "demo@slideforge.local",
            "SlideForge User",
            "Local User",
            new UserPreferenceResponse("中文", "专业、克制、信息密度适中", "16:9")
    );

    public UserProfileResponse getCurrentUser() {
        return currentUser;
    }

    public UserProfileResponse updateCurrentUser(UpdateUserProfileRequest request) {
        currentUser = new UserProfileResponse(
                currentUser.id(),
                currentUser.email(),
                request.nickname(),
                currentUser.role(),
                new UserPreferenceResponse(
                        request.defaultLanguage(),
                        request.defaultPptStyle(),
                        request.defaultCanvasRatio()
                )
        );
        return currentUser;
    }
}
