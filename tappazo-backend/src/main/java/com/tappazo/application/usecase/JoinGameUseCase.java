package com.tappazo.application.usecase;

import com.tappazo.application.port.out.GameNotificationPort;
import com.tappazo.application.port.out.GameParticipantRepositoryPort;
import com.tappazo.application.port.out.GameRepositoryPort;
import com.tappazo.domain.exception.DomainException;
import com.tappazo.domain.exception.InvalidGameStateException;
import com.tappazo.domain.model.Game;
import com.tappazo.domain.model.GameParticipant;
import com.tappazo.domain.model.GameState;
import com.tappazo.domain.model.ParticipantRole;

import java.util.Objects;
import java.util.Optional;
import java.util.UUID;

/**
 * Caso de uso: Unirse a una Partida (Sección 6, 7, 8, 57).
 * El rol asignado depende del estado actual del Game (LOBBY -> PLAYER, IN_PROGRESS -> SPECTATOR).
 */
public class JoinGameUseCase {

    private final GameRepositoryPort gameRepository;
    private final GameParticipantRepositoryPort participantRepository;
    private final GameNotificationPort notificationPort;

    public JoinGameUseCase(GameRepositoryPort gameRepository,
                           GameParticipantRepositoryPort participantRepository,
                           GameNotificationPort notificationPort) {
        this.gameRepository = Objects.requireNonNull(gameRepository, "gameRepository must not be null");
        this.participantRepository = Objects.requireNonNull(participantRepository, "participantRepository must not be null");
        this.notificationPort = Objects.requireNonNull(notificationPort, "notificationPort must not be null");
    }

    public record JoinGameCommand(
            String gameCode,
            String userId
    ) {}

    public record JoinGameResult(
            String participantId,
            String gameId,
            String userId,
            ParticipantRole role,
            GameState gameState
    ) {}

    public JoinGameResult execute(JoinGameCommand command) {
        Objects.requireNonNull(command, "command must not be null");
        Objects.requireNonNull(command.gameCode(), "gameCode must not be null");
        Objects.requireNonNull(command.userId(), "userId must not be null");

        Game game = gameRepository.findByCode(command.gameCode().trim().toUpperCase())
                .orElseThrow(() -> new DomainException("Partida no encontrada con el código: " + command.gameCode()));

        // Sección 11: si la partida está finalizada, se rechaza explícitamente
        if (game.getState() == GameState.FINISHED) {
            throw new InvalidGameStateException("La partida ya ha finalizado y no acepta nuevos jugadores");
        }

        // Idempotencia: si ya está unido, retorna su participación existente
        Optional<GameParticipant> existing = participantRepository.findByGameIdAndUserId(game.getId(), command.userId());
        if (existing.isPresent()) {
            GameParticipant p = existing.get();
            return new JoinGameResult(p.getId(), p.getGameId(), p.getUserId(), p.getRole(), game.getState());
        }

        // Límite de jugadores (Sección 7)
        int currentCount = participantRepository.countByGameId(game.getId());
        if (currentCount >= game.getMaxPlayers()) {
            throw new DomainException("La partida ha alcanzado el límite máximo de jugadores (" + game.getMaxPlayers() + ")");
        }

        // Rol según estado del juego (Sección 8)
        GameParticipant participant;
        String participantId = UUID.randomUUID().toString();

        if (game.getState() == GameState.LOBBY) {
            participant = GameParticipant.createPlayer(participantId, game.getId(), command.userId());
        } else {
            participant = GameParticipant.createSpectator(participantId, game.getId(), command.userId());
        }

        GameParticipant savedParticipant = participantRepository.save(participant);

        notificationPort.notifyPlayerJoined(game.getId(), savedParticipant);

        return new JoinGameResult(
                savedParticipant.getId(),
                savedParticipant.getGameId(),
                savedParticipant.getUserId(),
                savedParticipant.getRole(),
                game.getState()
        );
    }
}
