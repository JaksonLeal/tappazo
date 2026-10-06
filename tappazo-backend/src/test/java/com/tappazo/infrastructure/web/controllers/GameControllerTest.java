package com.tappazo.infrastructure.web.controllers;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.tappazo.application.port.out.GameParticipantRepositoryPort;
import com.tappazo.application.port.out.GameRepositoryPort;
import com.tappazo.application.port.out.UserRepositoryPort;
import com.tappazo.domain.model.Game;
import com.tappazo.domain.model.GameMode;
import com.tappazo.domain.model.User;
import com.tappazo.infrastructure.security.JwtTokenProvider;
import com.tappazo.infrastructure.web.dto.WebDTOs.CreateGameRequest;
import com.tappazo.infrastructure.web.dto.WebDTOs.JoinGameRequest;
import com.tappazo.infrastructure.web.dto.WebDTOs.UpdateGameSettingsRequest;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import java.time.Instant;
import java.util.UUID;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@DisplayName("GameController REST Integration Tests")
class GameControllerTest {

    @Autowired private MockMvc mockMvc;
    @Autowired private ObjectMapper objectMapper;
    @Autowired private JwtTokenProvider jwtTokenProvider;
    @Autowired private UserRepositoryPort userRepository;
    @Autowired private GameRepositoryPort gameRepository;
    @Autowired private GameParticipantRepositoryPort participantRepository;

    private String hostUserId;
    private String hostToken;
    private String playerUserId;
    private String playerToken;

    @BeforeEach
    void setUp() {
        hostUserId = UUID.randomUUID().toString();
        userRepository.save(new User(hostUserId, "g-host-" + hostUserId, "HostPlayer", "host@test.com", null, Instant.now(), Instant.now()));
        hostToken = jwtTokenProvider.generateToken(hostUserId, "host@test.com", "HostPlayer");

        playerUserId = UUID.randomUUID().toString();
        userRepository.save(new User(playerUserId, "g-player-" + playerUserId, "GuestPlayer", "guest@test.com", null, Instant.now(), Instant.now()));
        playerToken = jwtTokenProvider.generateToken(playerUserId, "guest@test.com", "GuestPlayer");
    }

    @Test
    @DisplayName("POST /api/v1/games creates a game and returns 201 Created")
    void createGame_success() throws Exception {
        CreateGameRequest request = new CreateGameRequest(5000L, GameMode.ULTIMO_PIERDE, 8);

        mockMvc.perform(post("/api/v1/games")
                        .header("Authorization", "Bearer " + hostToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").isString())
                .andExpect(jsonPath("$.code").isString())
                .andExpect(jsonPath("$.hostId").value(hostUserId))
                .andExpect(jsonPath("$.state").value("LOBBY"))
                .andExpect(jsonPath("$.currentDrinkPrice").value(5000))
                .andExpect(jsonPath("$.maxPlayers").value(8));
    }

    @Test
    @DisplayName("POST /api/v1/games with invalid drinkPrice returns 400 Bad Request with validation errors")
    void createGame_validationError() throws Exception {
        CreateGameRequest request = new CreateGameRequest(0L, GameMode.ULTIMO_PIERDE, 8);

        mockMvc.perform(post("/api/v1/games")
                        .header("Authorization", "Bearer " + hostToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.validationErrors.drinkPrice").exists());
    }

    @Test
    @DisplayName("POST /api/v1/games/join allows another authenticated player to join")
    void joinGame_success() throws Exception {
        // Host creates game
        String gameId = UUID.randomUUID().toString();
        String code = "JOIN01";
        Game game = new Game(gameId, code, hostUserId, 8, 4000L, GameMode.ULTIMO_PIERDE);
        gameRepository.save(game);

        JoinGameRequest request = new JoinGameRequest(code);

        mockMvc.perform(post("/api/v1/games/join")
                        .header("Authorization", "Bearer " + playerToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.gameId").value(gameId))
                .andExpect(jsonPath("$.userId").value(playerUserId))
                .andExpect(jsonPath("$.role").value("PLAYER"));
    }

    @Test
    @DisplayName("GET /api/v1/games/{id} returns complete game details")
    void getGameDetails_success() throws Exception {
        String gameId = UUID.randomUUID().toString();
        String code = "DET001";
        Game game = new Game(gameId, code, hostUserId, 6, 3500L, GameMode.PEQUENOS_PAGAN);
        gameRepository.save(game);

        mockMvc.perform(get("/api/v1/games/" + gameId)
                        .header("Authorization", "Bearer " + hostToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.game.id").value(gameId))
                .andExpect(jsonPath("$.game.code").value(code))
                .andExpect(jsonPath("$.game.currentMode").value("PEQUENOS_PAGAN"));
    }

    @Test
    @DisplayName("GET /api/v1/games/code/{code} returns game by code")
    void getGameByCode_success() throws Exception {
        String gameId = UUID.randomUUID().toString();
        String code = "CODE99";
        Game game = new Game(gameId, code, hostUserId, 6, 3500L, GameMode.ULTIMOS_DOS_PIERDEN);
        gameRepository.save(game);

        mockMvc.perform(get("/api/v1/games/code/" + code)
                        .header("Authorization", "Bearer " + hostToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(gameId))
                .andExpect(jsonPath("$.currentMode").value("ULTIMOS_DOS_PIERDEN"));
    }

    @Test
    @DisplayName("PATCH /api/v1/games/{id}/settings updates settings when caller is host")
    void updateSettings_asHost_success() throws Exception {
        String gameId = UUID.randomUUID().toString();
        String code = "SETT01";
        Game game = new Game(gameId, code, hostUserId, 6, 3000L, GameMode.ULTIMO_PIERDE);
        gameRepository.save(game);

        UpdateGameSettingsRequest request = new UpdateGameSettingsRequest(6000L, GameMode.PEQUENOS_PAGAN);

        mockMvc.perform(patch("/api/v1/games/" + gameId + "/settings")
                        .header("Authorization", "Bearer " + hostToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.currentDrinkPrice").value(6000))
                .andExpect(jsonPath("$.currentMode").value("PEQUENOS_PAGAN"));
    }

    @Test
    @DisplayName("PATCH /api/v1/games/{id}/settings rejects when caller is NOT host with 403")
    void updateSettings_nonHost_forbidden() throws Exception {
        String gameId = UUID.randomUUID().toString();
        String code = "SETT02";
        Game game = new Game(gameId, code, hostUserId, 6, 3000L, GameMode.ULTIMO_PIERDE);
        gameRepository.save(game);

        UpdateGameSettingsRequest request = new UpdateGameSettingsRequest(6000L, GameMode.PEQUENOS_PAGAN);

        mockMvc.perform(patch("/api/v1/games/" + gameId + "/settings")
                        .header("Authorization", "Bearer " + playerToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isForbidden());
    }

    @Test
    @DisplayName("POST /api/v1/games/{id}/voice-token returns LiveKit VoiceToken for participant")
    void getVoiceToken_success() throws Exception {
        String gameId = UUID.randomUUID().toString();
        String code = "VOIC01";
        Game game = new Game(gameId, code, hostUserId, 6, 3000L, GameMode.ULTIMO_PIERDE);
        gameRepository.save(game);
        participantRepository.save(new com.tappazo.domain.model.GameParticipant(
                UUID.randomUUID().toString(), gameId, hostUserId,
                com.tappazo.domain.model.ParticipantRole.PLAYER,
                com.tappazo.domain.model.ParticipantState.ACTIVE,
                Instant.now()
        ));

        mockMvc.perform(post("/api/v1/games/" + gameId + "/voice-token")
                        .header("Authorization", "Bearer " + hostToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.token").isNotEmpty())
                .andExpect(jsonPath("$.roomName").value("game-" + gameId))
                .andExpect(jsonPath("$.serverUrl").value("ws://localhost:7880"));
    }

    @Test
    @DisplayName("POST /api/v1/games/{id}/voice-token returns 403 when user is not a participant")
    void getVoiceToken_nonParticipant_forbidden() throws Exception {
        String gameId = UUID.randomUUID().toString();
        String code = "VOIC02";
        Game game = new Game(gameId, code, hostUserId, 6, 3000L, GameMode.ULTIMO_PIERDE);
        gameRepository.save(game);

        mockMvc.perform(post("/api/v1/games/" + gameId + "/voice-token")
                        .header("Authorization", "Bearer " + playerToken))
                .andExpect(status().isForbidden());
    }
}
