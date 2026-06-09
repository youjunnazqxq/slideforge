package com.slideforge.api.auth;

import com.slideforge.api.auth.dto.MockLoginRequest;
import com.slideforge.api.auth.dto.MockLoginResponse;
import com.slideforge.api.auth.dto.LoginRequest;
import com.slideforge.api.auth.dto.LoginResponse;
import com.slideforge.api.common.response.ApiResponse;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class AuthController {

    private final AuthService authService;

    public AuthController(AuthService authService) {
        this.authService = authService;
    }

    @PostMapping("/api/auth/mock-login")
    public ApiResponse<MockLoginResponse> mockLogin(@Valid @RequestBody MockLoginRequest request) {
        return ApiResponse.success(authService.mockLogin(request));
    }

    @PostMapping("/login")
    public ApiResponse<LoginResponse> login(@Valid @RequestBody LoginRequest request) {
        return ApiResponse.success(authService.login(request));
    }
}
