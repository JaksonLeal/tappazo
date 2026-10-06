package com.tappazo.infrastructure.web.controllers;

import com.tappazo.application.port.out.UserRepositoryPort;
import com.tappazo.domain.model.User;
import com.tappazo.infrastructure.security.GoogleAuthService;
import com.tappazo.infrastructure.security.JwtTokenProvider;
import com.tappazo.infrastructure.web.dto.WebDTOs.AuthResponse;
import com.tappazo.infrastructure.web.dto.WebDTOs.GoogleAuthRequest;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.time.Instant;
import java.util.UUID;

@RestController
@RequestMapping({"/api/v1/auth", "/auth"})
public class AuthController {

    private final GoogleAuthService googleAuthService;
    private final JwtTokenProvider jwtTokenProvider;
    private final UserRepositoryPort userRepository;

    public AuthController(
            GoogleAuthService googleAuthService,
            JwtTokenProvider jwtTokenProvider,
            UserRepositoryPort userRepository) {
        this.googleAuthService = googleAuthService;
        this.jwtTokenProvider  = jwtTokenProvider;
        this.userRepository    = userRepository;
    }

    @PostMapping("/google")
    public ResponseEntity<AuthResponse> authenticateWithGoogle(
            @Valid @RequestBody GoogleAuthRequest request) {

        // 1. Validar Google ID Token
        GoogleAuthService.GoogleUserInfo googleUser =
                googleAuthService.verifyGoogleToken(request.idToken());

        // 2. Buscar o crear usuario de dominio
        User user = userRepository.findByGoogleId(googleUser.googleId())
                .orElseGet(() -> {
                    Instant now = Instant.now();
                    User newUser = new User(
                            UUID.randomUUID().toString(),
                            googleUser.googleId(),
                            googleUser.name(),
                            googleUser.email(),
                            null,
                            now,
                            now);
                    return userRepository.save(newUser);
                });

        // 3. Generar JWT propio de Tappazo
        String tappazoToken = jwtTokenProvider.generateToken(
                user.getId(), user.getEmail(), user.getNickname());

        // 4. Retornar token y datos del usuario
        return ResponseEntity.ok(new AuthResponse(
                tappazoToken,
                user.getId(),
                user.getNickname(),
                user.getEmail()));
    }
}
