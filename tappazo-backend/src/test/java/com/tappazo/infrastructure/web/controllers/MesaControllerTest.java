package com.tappazo.infrastructure.web.controllers;

import com.tappazo.application.port.out.*;
import com.tappazo.domain.model.*;
import com.tappazo.infrastructure.security.JwtTokenProvider;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.web.servlet.MockMvc;

import java.time.Instant;
import java.util.UUID;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@DisplayName("MesaController REST Integration Tests")
class MesaControllerTest {

    @Autowired private MockMvc mockMvc;
    @Autowired private JwtTokenProvider jwtTokenProvider;
    @Autowired private UserRepositoryPort userRepository;
    @Autowired private GameRepositoryPort gameRepository;
    @Autowired private GameParticipantRepositoryPort participantRepository;
    @Autowired private DebtMovementRepositoryPort debtMovementRepository;
    @Autowired private PlayerStatisticsRepositoryPort statisticsRepository;

    private String userAId;
    private String userBId;
    private String userAToken;

    @BeforeEach
    void setUp() {
        userAId = UUID.randomUUID().toString();
        userBId = UUID.randomUUID().toString();

        userRepository.save(new User(userAId, "g-mesa-A-" + userAId, "UserA", "usera@test.com", null, Instant.now(), Instant.now()));
        userRepository.save(new User(userBId, "g-mesa-B-" + userBId, "UserB", "userb@test.com", null, Instant.now(), Instant.now()));

        userAToken = jwtTokenProvider.generateToken(userAId, "usera@test.com", "UserA");
    }

    @Test
    @DisplayName("GET /api/v1/mesas/{gameId} returns mesa info and participants")
    void getMesa_success() throws Exception {
        String gameId = UUID.randomUUID().toString();
        Game game = new Game(gameId, "MESA01", userAId, 6, 4000L, GameMode.ULTIMO_PIERDE);
        gameRepository.save(game);

        participantRepository.save(GameParticipant.createPlayer(UUID.randomUUID().toString(), gameId, userAId));
        participantRepository.save(GameParticipant.createPlayer(UUID.randomUUID().toString(), gameId, userBId));

        mockMvc.perform(get("/api/v1/mesas/" + gameId)
                        .header("Authorization", "Bearer " + userAToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.gameId").value(gameId))
                .andExpect(jsonPath("$.code").value("MESA01"))
                .andExpect(jsonPath("$.playerCount").value(2));
    }

    @Test
    @DisplayName("GET /api/v1/mesas/{gameId}/balances returns calculated net debts between players")
    void getMesaBalances_success() throws Exception {
        String gameId = UUID.randomUUID().toString();
        Game game = new Game(gameId, "BAL001", userAId, 6, 4000L, GameMode.ULTIMO_PIERDE);
        gameRepository.save(game);

        String roundId = UUID.randomUUID().toString();

        // userA debe a userB: 4000, userB debe a userA: 1000 => saldo neto: userA debe a userB: 3000
        debtMovementRepository.save(new DebtMovement(UUID.randomUUID().toString(), gameId, roundId, userAId, userBId, 4000L, DebtReason.ROUND_LOSS, Instant.now()));
        debtMovementRepository.save(new DebtMovement(UUID.randomUUID().toString(), gameId, roundId, userBId, userAId, 1000L, DebtReason.ROUND_LOSS, Instant.now()));

        mockMvc.perform(get("/api/v1/mesas/" + gameId + "/balances")
                        .header("Authorization", "Bearer " + userAToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.gameId").value(gameId))
                .andExpect(jsonPath("$.totalActiveDebt").value(3000))
                .andExpect(jsonPath("$.netDebts[0].fromPlayerId").value(userAId))
                .andExpect(jsonPath("$.netDebts[0].toPlayerId").value(userBId))
                .andExpect(jsonPath("$.netDebts[0].amount").value(3000));
    }

    @Test
    @DisplayName("GET /api/v1/mesas/{gameId}/debts returns all raw debt movements")
    void getMesaDebts_success() throws Exception {
        String gameId = UUID.randomUUID().toString();
        Game game = new Game(gameId, "DEBT01", userAId, 6, 4000L, GameMode.ULTIMO_PIERDE);
        gameRepository.save(game);

        String roundId = UUID.randomUUID().toString();
        debtMovementRepository.save(new DebtMovement(UUID.randomUUID().toString(), gameId, roundId, userAId, userBId, 4000L, DebtReason.ROUND_LOSS, Instant.now()));

        mockMvc.perform(get("/api/v1/mesas/" + gameId + "/debts")
                        .header("Authorization", "Bearer " + userAToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$").isArray())
                .andExpect(jsonPath("$[0].gameId").value(gameId))
                .andExpect(jsonPath("$[0].amount").value(4000));
    }

    @Test
    @DisplayName("GET /api/v1/mesas/users/{userId}/statistics returns player stats")
    void getUserStatistics_success() throws Exception {
        PlayerStatistics stats = new PlayerStatistics(userAId, 12, 8, 4, 16000L, 32000L);
        statisticsRepository.save(stats);

        mockMvc.perform(get("/api/v1/mesas/users/" + userAId + "/statistics")
                        .header("Authorization", "Bearer " + userAToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.userId").value(userAId))
                .andExpect(jsonPath("$.roundsPlayed").value(12))
                .andExpect(jsonPath("$.roundsWon").value(8))
                .andExpect(jsonPath("$.roundsLost").value(4))
                .andExpect(jsonPath("$.moneySpent").value(16000))
                .andExpect(jsonPath("$.moneyReceived").value(32000));
    }
}
