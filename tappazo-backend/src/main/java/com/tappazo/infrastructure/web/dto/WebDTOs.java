package com.tappazo.infrastructure.web.dto;

import com.tappazo.domain.model.GameMode;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

import java.time.Instant;
import java.util.List;

public final class WebDTOs {

    private WebDTOs() {}

    // ─── Auth DTOs ────────────────────────────────────────────────────────────

    public record GoogleAuthRequest(
            @NotBlank(message = "El idToken no puede estar vacío")
            String idToken
    ) {}

    public record AuthResponse(
            String token,
            String userId,
            String nickname,
            String email
    ) {}

    // ─── Game DTOs ────────────────────────────────────────────────────────────

    public record CreateGameRequest(
            @Min(value = 1, message = "El precio de la bebida debe ser mayor a 0")
            long drinkPrice,

            GameMode initialMode,

            @Min(value = 2, message = "El mínimo de jugadores permitidos es 2")
            @Max(value = 10, message = "El máximo de jugadores permitidos es 10")
            Integer maxPlayers
    ) {}

    public record JoinGameRequest(
            @NotBlank(message = "El código de partida no puede estar vacío")
            @Size(min = 4, max = 10, message = "El código debe tener entre 4 y 10 caracteres")
            String gameCode
    ) {}

    public record UpdateGameSettingsRequest(
            @Min(value = 1, message = "El precio debe ser mayor a 0")
            Long drinkPrice,

            GameMode mode
    ) {}

    public record GameResponse(
            String id,
            String code,
            String hostId,
            String state,
            int maxPlayers,
            long currentDrinkPrice,
            String currentMode,
            Instant createdAt
    ) {}

    public record GameParticipantResponse(
            String id,
            String gameId,
            String userId,
            String role,
            String participantState,
            Instant joinedAt
    ) {}

    public record RoundResponse(
            String id,
            String gameId,
            String mode,
            String state,
            long drinkPrice,
            String roundType,
            Instant startedAt,
            Instant finishedAt
    ) {}

    public record GameDetailsResponse(
            GameResponse game,
            List<GameParticipantResponse> participants,
            RoundResponse currentRound
    ) {}

    // ─── Mesa / Group DTOs ────────────────────────────────────────────────────

    public record MesaResponse(
            String gameId,
            String code,
            String hostId,
            String state,
            int playerCount,
            List<GameParticipantResponse> participants
    ) {}

    public record NetDebtResponse(
            String fromPlayerId,
            String toPlayerId,
            long amount
    ) {}

    public record MesaBalanceResponse(
            String gameId,
            List<NetDebtResponse> netDebts,
            long totalActiveDebt
    ) {}

    public record DebtMovementResponse(
            String id,
            String gameId,
            String roundId,
            String fromPlayerId,
            String toPlayerId,
            long amount,
            String reason,
            Instant createdAt
    ) {}

    public record PlayerStatisticsResponse(
            String userId,
            int roundsPlayed,
            int roundsWon,
            int roundsLost,
            long moneySpent,
            long moneyReceived
    ) {}

    // ─── Voice / LiveKit DTOs ─────────────────────────────────────────────────

    public record VoiceTokenResponse(
            String token,
            String roomName,
            String serverUrl
    ) {}

    // ─── Media / Storage DTOs ─────────────────────────────────────────────────

    public record PresignedUrlRequest(
            @NotBlank(message = "El nombre de archivo no puede estar vacío")
            String fileName,
            String contentType,
            String folder
    ) {}

    public record PresignedUrlResponse(
            String uploadUrl,
            String fileKey,
            String downloadUrl
    ) {}
}
