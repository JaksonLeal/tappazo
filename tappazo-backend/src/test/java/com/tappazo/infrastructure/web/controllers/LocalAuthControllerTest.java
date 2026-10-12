package com.tappazo.infrastructure.web.controllers;

import com.tappazo.application.port.out.UserRepositoryPort;
import com.tappazo.domain.model.User;
import com.tappazo.infrastructure.security.JwtTokenProvider;
import com.tappazo.infrastructure.web.dto.WebDTOs.AuthResponse;
import com.tappazo.infrastructure.web.dto.WebDTOs.LocalAuthRequest;
import org.junit.jupiter.api.Test;
import org.springframework.http.ResponseEntity;

import java.time.Instant;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class LocalAuthControllerTest {

    private final UserRepositoryPort userRepository = mock(UserRepositoryPort.class);
    private final JwtTokenProvider jwtTokenProvider = mock(JwtTokenProvider.class);
    private final LocalAuthController controller = new LocalAuthController(userRepository, jwtTokenProvider);

    @Test
    void authenticateLocalPlayer_createsUserAndReturnsTappazoToken() {
        when(userRepository.findByEmail("player1@example.test")).thenReturn(Optional.empty());
        when(userRepository.save(any(User.class))).thenAnswer(invocation -> invocation.getArgument(0));
        when(jwtTokenProvider.generateToken(any(), any(), any())).thenReturn("test-jwt");

        ResponseEntity<AuthResponse> response = controller.authenticateLocalPlayer(
                new LocalAuthRequest(" Player1@Example.Test ", " Player One "));

        assertThat(response.getStatusCode().value()).isEqualTo(200);
        assertThat(response.getBody()).isNotNull();
        assertThat(response.getBody().token()).isEqualTo("test-jwt");
        assertThat(response.getBody().email()).isEqualTo("player1@example.test");
        assertThat(response.getBody().nickname()).isEqualTo("Player One");
        verify(userRepository).save(any(User.class));
        verify(jwtTokenProvider).generateToken(response.getBody().userId(), "player1@example.test", "Player One");
    }

    @Test
    void authenticateLocalPlayer_reusesExistingUserByEmail() {
        User existingUser = new User(
                "existing-user-id",
                "local:existing-id",
                "Existing Player",
                "player@example.test",
                null,
                Instant.now(),
                Instant.now());
        when(userRepository.findByEmail("player@example.test")).thenReturn(Optional.of(existingUser));
        when(jwtTokenProvider.generateToken("existing-user-id", "player@example.test", "Existing Player"))
                .thenReturn("existing-jwt");

        ResponseEntity<AuthResponse> response = controller.authenticateLocalPlayer(
                new LocalAuthRequest("player@example.test", "Ignored Nickname"));

        assertThat(response.getBody()).isNotNull();
        assertThat(response.getBody().userId()).isEqualTo("existing-user-id");
        assertThat(response.getBody().nickname()).isEqualTo("Existing Player");
        assertThat(response.getBody().token()).isEqualTo("existing-jwt");
        verify(userRepository, never()).save(any(User.class));
    }
}
