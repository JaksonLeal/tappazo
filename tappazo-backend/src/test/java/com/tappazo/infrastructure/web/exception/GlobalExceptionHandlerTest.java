package com.tappazo.infrastructure.web.exception;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.tappazo.domain.exception.DomainException;
import com.tappazo.domain.exception.InvalidGameStateException;
import com.tappazo.infrastructure.security.JwtTokenProvider;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@Import(GlobalExceptionHandlerTest.TestExceptionController.class)
@DisplayName("GlobalExceptionHandler Unit & Integration Tests")
class GlobalExceptionHandlerTest {

    @Autowired private MockMvc mockMvc;
    @Autowired private ObjectMapper objectMapper;
    @Autowired private JwtTokenProvider jwtTokenProvider;

    public record DummyValidationRequest(
            @NotBlank(message = "El nombre es obligatorio")
            String name,

            @NotNull(message = "La edad es obligatoria")
            Integer age
    ) {}

    @RestController
    static class TestExceptionController {
        @GetMapping("/api/test/domain-exception")
        public void throwDomainException() {
            throw new DomainException("Regla de negocio violada intencionalmente");
        }

        @GetMapping("/api/test/invalid-state")
        public void throwInvalidState() {
            throw new InvalidGameStateException("Partida en estado no permitido");
        }

        @PostMapping("/api/test/validate")
        public String validateBody(@Valid @RequestBody DummyValidationRequest request) {
            return "OK";
        }
    }

    @Test
    @DisplayName("DomainException returns HTTP 400 Bad Request with formatted ErrorResponse")
    void handleDomainException_returns400() throws Exception {
        String token = jwtTokenProvider.generateToken("user-test", "test@test.com", "TestUser");

        mockMvc.perform(get("/api/test/domain-exception")
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.status").value(400))
                .andExpect(jsonPath("$.error").value("Regla de negocio no satisfecha"))
                .andExpect(jsonPath("$.message").value("Regla de negocio violada intencionalmente"))
                .andExpect(jsonPath("$.path").value("/api/test/domain-exception"))
                .andExpect(jsonPath("$.timestamp").exists());
    }

    @Test
    @DisplayName("InvalidGameStateException returns HTTP 409 Conflict with formatted ErrorResponse")
    void handleInvalidGameState_returns409() throws Exception {
        String token = jwtTokenProvider.generateToken("user-test", "test@test.com", "TestUser");

        mockMvc.perform(get("/api/test/invalid-state")
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.status").value(409))
                .andExpect(jsonPath("$.error").value("Estado de partida inválido"))
                .andExpect(jsonPath("$.message").value("Partida en estado no permitido"))
                .andExpect(jsonPath("$.path").value("/api/test/invalid-state"));
    }

    @Test
    @DisplayName("MethodArgumentNotValidException returns HTTP 400 Bad Request with field errors map")
    void handleValidationException_returns400WithErrors() throws Exception {
        String token = jwtTokenProvider.generateToken("user-test", "test@test.com", "TestUser");
        DummyValidationRequest invalidRequest = new DummyValidationRequest("", null);

        mockMvc.perform(post("/api/test/validate")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(invalidRequest)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.status").value(400))
                .andExpect(jsonPath("$.validationErrors.name").value("El nombre es obligatorio"))
                .andExpect(jsonPath("$.validationErrors.age").value("La edad es obligatoria"));
    }
}
