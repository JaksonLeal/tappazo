package com.tappazo.infrastructure.security;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.tappazo.application.port.out.UserRepositoryPort;
import com.tappazo.domain.exception.DomainException;
import com.tappazo.infrastructure.web.controllers.AuthController;
import com.tappazo.infrastructure.web.dto.WebDTOs.GoogleAuthRequest;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@org.springframework.context.annotation.Import(SecurityIntegrationTest.TestProtectedController.class)
@DisplayName("Security & Authentication Integration Tests")
class SecurityIntegrationTest {

    @Autowired private MockMvc mockMvc;
    @Autowired private ObjectMapper objectMapper;
    @Autowired private JwtTokenProvider jwtTokenProvider;
    @Autowired private UserRepositoryPort userRepository;

    @MockBean
    private GoogleAuthService googleAuthService;

    @RestController
    static class TestProtectedController {
        @GetMapping("/api/test/protected")
        public ResponseEntity<Map<String, String>> protectedEndpoint(@AuthenticationPrincipal UserPrincipal principal) {
            return ResponseEntity.ok(Map.of(
                    "userId", principal.getId(),
                    "email", principal.getEmail()
            ));
        }
    }

    @Test
    @DisplayName("Public endpoint /auth/google allows unauthenticated POST")
    void authGoogle_isPublic() throws Exception {
        when(googleAuthService.verifyGoogleToken("valid-id-token"))
                .thenReturn(new GoogleAuthService.GoogleUserInfo("google-sub-1", "user1@gmail.com", "User One"));

        GoogleAuthRequest request = new GoogleAuthRequest("valid-id-token");

        mockMvc.perform(post("/auth/google")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.token").isString())
                .andExpect(jsonPath("$.nickname").value("User One"))
                .andExpect(jsonPath("$.email").value("user1@gmail.com"));

        assertThat(userRepository.findByGoogleId("google-sub-1")).isPresent();
    }

    @Test
    @DisplayName("Subsequent /auth/google call reuses existing user")
    void authGoogle_reusesExistingUser() throws Exception {
        when(googleAuthService.verifyGoogleToken("valid-id-token-2"))
                .thenReturn(new GoogleAuthService.GoogleUserInfo("google-sub-2", "user2@gmail.com", "User Two"));

        GoogleAuthRequest request = new GoogleAuthRequest("valid-id-token-2");

        // First call
        mockMvc.perform(post("/auth/google")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk());

        String userIdFirst = userRepository.findByGoogleId("google-sub-2").orElseThrow().getId();

        // Second call
        mockMvc.perform(post("/auth/google")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.userId").value(userIdFirst));
    }

    @Test
    @DisplayName("Protected endpoint rejects request without Bearer token with 401")
    void protectedEndpoint_withoutToken_returns401() throws Exception {
        mockMvc.perform(get("/api/test/protected"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    @DisplayName("Protected endpoint rejects request with invalid Bearer token with 401")
    void protectedEndpoint_withInvalidToken_returns401() throws Exception {
        mockMvc.perform(get("/api/test/protected")
                        .header("Authorization", "Bearer invalid-jwt-token-xyz"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    @DisplayName("Protected endpoint allows request with valid Bearer token and injects UserPrincipal")
    void protectedEndpoint_withValidToken_returns200AndPrincipal() throws Exception {
        String token = jwtTokenProvider.generateToken("user-sec-99", "sec99@tappazo.com", "SecPlayer");

        mockMvc.perform(get("/api/test/protected")
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.userId").value("user-sec-99"))
                .andExpect(jsonPath("$.email").value("sec99@tappazo.com"));
    }
}
