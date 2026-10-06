package com.tappazo.infrastructure.presentation;

import com.tappazo.application.port.out.UserRepositoryPort;
import com.tappazo.domain.model.User;
import com.tappazo.infrastructure.security.GoogleAuthService;
import com.tappazo.infrastructure.security.JwtTokenProvider;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.Instant;
import java.util.UUID;

/**
 * Authentication controller — Sección 3 (Auth flow).
 *
 * POST /auth/google
 *   1. Receives Google ID Token from Flutter client.
 *   2. Delegates to GoogleAuthService to verify and extract user data.
 *   3. Finds or creates the User in the database via UserRepositoryPort.
 *   4. Generates a Tappazo JWT for subsequent REST + WebSocket calls.
 *   5. Returns the JWT to the client.
 */
@RestController
@RequestMapping("/auth")
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

    /** Request body from Flutter: raw Google ID Token string. */
    public record GoogleAuthRequest(String idToken) {}

    /** Response body: Tappazo JWT + basic user info. */
    public record AuthResponse(String token, String userId, String nickname, String email) {}

    @PostMapping("/google")
    public ResponseEntity<AuthResponse> authenticateWithGoogle(
            @RequestBody GoogleAuthRequest request) {

        // 1. Verify Google token
        GoogleAuthService.GoogleUserInfo googleUser =
                googleAuthService.verifyGoogleToken(request.idToken());

        // 2. Find or create user
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

        // 3. Generate Tappazo JWT
        String tappazoToken = jwtTokenProvider.generateToken(
                user.getId(), user.getEmail(), user.getNickname());

        // 4. Return
        return ResponseEntity.ok(new AuthResponse(
                tappazoToken,
                user.getId(),
                user.getNickname(),
                user.getEmail()));
    }
}
