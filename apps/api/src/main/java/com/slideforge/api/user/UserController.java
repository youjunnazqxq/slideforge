package com.slideforge.api.user;

import com.slideforge.api.common.response.ApiResponse;
import com.slideforge.api.user.dto.UpdateUserProfileRequest;
import com.slideforge.api.user.dto.UserProfileResponse;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/users")
public class UserController {

    private final UserService userService;

    public UserController(UserService userService) {
        this.userService = userService;
    }

    @GetMapping("/me")
    public ApiResponse<UserProfileResponse> getCurrentUser() {
        return ApiResponse.success(userService.getCurrentUser());
    }

    @GetMapping("/profile")
    public ApiResponse<UserProfileResponse> getProfile() {
        return ApiResponse.success(userService.getCurrentUser());
    }

    @PutMapping("/me")
    public ApiResponse<UserProfileResponse> updateCurrentUser(@Valid @RequestBody UpdateUserProfileRequest request) {
        return ApiResponse.success(userService.updateCurrentUser(request));
    }
}
