package com.tappazo.infrastructure.security;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

@DisplayName("JwtTokenProvider Unit Tests")
class JwtTokenProviderTest {

    private static final String SECRET = "tappazo-test-secret-key-that-is-at-least-256-bits-long-for-hmac-sha256!";
    private static final long EXPIRATION_MS = 3600000; // 1 hour

    private JwtTokenProvider jwtTokenProvider;

    @BeforeEach
    void setUp() {
        jwtTokenProvider = new JwtTokenProvider(SECRET, EXPIRATION_MS);
    }

    @Test
    @DisplayName("generateToken produces a valid non-empty JWT")
    void generateToken_producesValidToken() {
        String token = jwtTokenProvider.generateToken("user-123", "test@tappazo.com", "ElChapa");

        assertThat(token).isNotBlank();
        assertThat(jwtTokenProvider.validateToken(token)).isTrue();
    }

    @Test
    @DisplayName("getUserIdFromToken extracts the correct subject")
    void getUserIdFromToken_extractsSubject() {
        String token = jwtTokenProvider.generateToken("user-456", "player@tappazo.com", "Chelero");

        String userId = jwtTokenProvider.getUserIdFromToken(token);
        assertThat(userId).isEqualTo("user-456");
    }

    @Test
    @DisplayName("getEmailFromToken and getNicknameFromToken extract claims correctly")
    void getClaims_extractsCustomClaims() {
        String token = jwtTokenProvider.generateToken("user-789", "corona@tappazo.com", "ElTapas");

        assertThat(jwtTokenProvider.getEmailFromToken(token)).isEqualTo("corona@tappazo.com");
        assertThat(jwtTokenProvider.getNicknameFromToken(token)).isEqualTo("ElTapas");
    }

    @Test
    @DisplayName("validateToken returns false for a tampered token")
    void validateToken_tamperedToken_returnsFalse() {
        String token = jwtTokenProvider.generateToken("user-123", "test@tappazo.com", "ElChapa");
        String tampered = token + "xyz";

        assertThat(jwtTokenProvider.validateToken(tampered)).isFalse();
    }

    @Test
    @DisplayName("validateToken returns false for malformed token string")
    void validateToken_malformedToken_returnsFalse() {
        assertThat(jwtTokenProvider.validateToken("not-a-valid-jwt")).isFalse();
        assertThat(jwtTokenProvider.validateToken("")).isFalse();
    }

    @Test
    @DisplayName("validateToken returns false for expired token")
    void validateToken_expiredToken_returnsFalse() {
        JwtTokenProvider expiredProvider = new JwtTokenProvider(SECRET, -1000); // already expired
        String token = expiredProvider.generateToken("user-exp", "exp@tappazo.com", "ExpiredUser");

        assertThat(jwtTokenProvider.validateToken(token)).isFalse();
    }
}
