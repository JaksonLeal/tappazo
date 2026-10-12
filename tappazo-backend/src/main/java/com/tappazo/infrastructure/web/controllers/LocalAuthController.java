package com.tappazo.infrastructure.web.controllers;

import com.tappazo.application.port.out.UserRepositoryPort;
import com.tappazo.domain.model.User;
import com.tappazo.infrastructure.security.JwtTokenProvider;
import com.tappazo.infrastructure.web.dto.WebDTOs.AuthResponse;
import com.tappazo.infrastructure.web.dto.WebDTOs.LocalAuthRequest;
import jakarta.validation.Valid;
import org.springframework.context.annotation.Profile;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.time.Instant;
import java.util.Locale;
import java.util.UUID;

@RestController
@Profile("local-auth")
@RequestMapping("/api/v1/auth/local")
public class LocalAuthController {

    private final UserRepositoryPort userRepository;
    private final JwtTokenProvider jwtTokenProvider;

    public LocalAuthController(UserRepositoryPort userRepository, JwtTokenProvider jwtTokenProvider) {
        this.userRepository = userRepository;
        this.jwtTokenProvider = jwtTokenProvider;
    }

    @PostMapping
    public ResponseEntity<AuthResponse> authenticateLocalPlayer(
            @Valid @RequestBody LocalAuthRequest request) {
        String email = request.email().trim().toLowerCase(Locale.ROOT);
        String nickname = request.nickname().trim();

        User user = userRepository.findByEmail(email)
                .orElseGet(() -> {
                    Instant now = Instant.now();
                    User newUser = new User(
                            UUID.randomUUID().toString(),
                            "local:" + UUID.randomUUID(),
                            nickname,
                            email,
                            null,
                            now,
                            now);
                    return userRepository.save(newUser);
                });

        String token = jwtTokenProvider.generateToken(
                user.getId(), user.getEmail(), user.getNickname());

        return ResponseEntity.ok(new AuthResponse(
                token,
                user.getId(),
                user.getNickname(),
                user.getEmail()));
    }
}
