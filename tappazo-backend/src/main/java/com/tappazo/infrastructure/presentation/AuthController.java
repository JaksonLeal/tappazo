package com.tappazo.infrastructure.presentation;

import com.tappazo.infrastructure.persistence.entity.UserEntity;
import com.tappazo.infrastructure.persistence.repository.JpaUserRepository;
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
 *   3. Finds or creates the User in the database.
 *   4. Generates a Tappazo JWT for subsequent REST + WebSocket calls.
 *   5. Returns the JWT to the client.
 */
@RestController
@RequestMapping("/auth")
public class AuthController {

    private final GoogleAuthService googleAuthService;
    private final JwtTokenProvider jwtTokenProvider;
    private final JpaRepositories.JpaUserRepository userRepository;

    public AuthController(
            GoogleAuthService googleAuthService,
            JwtTokenProvider jwtTokenProvider,
            JpaRepositories.JpaUserRepository userRepository) {
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
        UserEntity entity = userRepository.findByGoogleId(googleUser.googleId())
                .orElseGet(() -> {
                    Instant now = Instant.now();
                    UserEntity newUser = new UserEntity();
                    newUser.setId(UUID.randomUUID().toString());
                    newUser.setGoogleId(googleUser.googleId());
                    newUser.setEmail(googleUser.email());
                    newUser.setNickname(googleUser.name());
                    newUser.setCreatedAt(now);
                    newUser.setUpdatedAt(now);
                    return userRepository.save(newUser);
                });

        // 3. Generate Tappazo JWT
        String tappazoToken = jwtTokenProvider.generateToken(
                entity.getId(), entity.getEmail(), entity.getNickname());

        // 4. Return
        return ResponseEntity.ok(new AuthResponse(
                tappazoToken,
                entity.getId(),
                entity.getNickname(),
                entity.getEmail()));
    }
}
