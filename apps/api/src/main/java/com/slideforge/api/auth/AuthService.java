package com.slideforge.api.auth;

import com.slideforge.api.auth.dto.AuthUserResponse;
import com.slideforge.api.auth.dto.LoginRequest;
import com.slideforge.api.auth.dto.LoginResponse;
import com.slideforge.api.auth.dto.MockLoginRequest;
import com.slideforge.api.auth.dto.MockLoginResponse;
import java.util.UUID;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;
import org.springframework.http.HttpStatus;

@Service
public class AuthService {

    private final UserAccountRepository userAccountRepository;
    private final PasswordEncoder passwordEncoder;

    public AuthService(UserAccountRepository userAccountRepository, PasswordEncoder passwordEncoder) {
        this.userAccountRepository = userAccountRepository;
        this.passwordEncoder = passwordEncoder;
    }

    public MockLoginResponse mockLogin(MockLoginRequest request) {
        String nickname = buildNickname(request.email());
        AuthUserResponse user = new AuthUserResponse("local-user", request.email(), nickname, "Local User");
        return new MockLoginResponse("mock-" + UUID.randomUUID(), user);
    }

    public LoginResponse login(LoginRequest request) {
        UserAccount user = userAccountRepository.findByUsername(request.username())
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.UNAUTHORIZED, "用户名或密码错误"));

        if (!"active".equalsIgnoreCase(user.getStatus())
                || !passwordEncoder.matches(request.password(), user.getPasswordHash())) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "用户名或密码错误");
        }

        return new LoginResponse(user.getUsername(), "mock-" + UUID.randomUUID());
    }

    private String buildNickname(String email) {
        int atIndex = email.indexOf("@");
        if (atIndex <= 0) {
            return "SlideForge User";
        }
        return email.substring(0, atIndex);
    }
}
