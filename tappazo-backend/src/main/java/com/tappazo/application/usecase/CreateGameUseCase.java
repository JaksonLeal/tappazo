package com.tappazo.application.usecase;

import com.tappazo.application.port.out.GameParticipantRepositoryPort;
import com.tappazo.application.port.out.GameRepositoryPort;
import com.tappazo.domain.model.Game;
import com.tappazo.domain.model.GameMode;
import com.tappazo.domain.model.GameParticipant;

import java.security.SecureRandom;
import java.util.Objects;
import java.util.UUID;

/**
 * Caso de uso: Crear Partida (Sección 6, 7, 9, 10, 56).
 * El creador se vuelve host y la partida inicia en LOBBY.
 */
public class CreateGameUseCase {

    private static final String CODE_CHARS = "ABCDEFGHJKLMNPQRSTUVWXYZ23456789";
    private static final int CODE_LENGTH = 6;
    private final SecureRandom random = new SecureRandom();

    private final GameRepositoryPort gameRepository;
    private final GameParticipantRepositoryPort participantRepository;

    public CreateGameUseCase(GameRepositoryPort gameRepository, GameParticipantRepositoryPort participantRepository) {
        this.gameRepository = Objects.requireNonNull(gameRepository, "gameRepository must not be null");
        this.participantRepository = Objects.requireNonNull(participantRepository, "participantRepository must not be null");
    }

    public record CreateGameCommand(
            String hostId,
            long drinkPrice,
            GameMode initialMode,
            Integer maxPlayers
    ) {}

    public record CreateGameResult(
            String gameId,
            String gameCode,
            String hostId,
            long drinkPrice,
            GameMode mode,
            int maxPlayers
    ) {}

    public CreateGameResult execute(CreateGameCommand command) {
        Objects.requireNonNull(command, "command must not be null");
        Objects.requireNonNull(command.hostId(), "hostId must not be null");

        String gameId = UUID.randomUUID().toString();
        String gameCode = generateUniqueCode();
        int maxPlayers = command.maxPlayers() != null ? command.maxPlayers() : Game.DEFAULT_MAX_PLAYERS;
        GameMode mode = command.initialMode() != null ? command.initialMode() : GameMode.ULTIMO_PIERDE;

        Game game = new Game(gameId, gameCode, command.hostId(), maxPlayers, command.drinkPrice(), mode);
        Game savedGame = gameRepository.save(game);

        // Registrar al host como PLAYER activo inicial (Sección 9)
        GameParticipant hostParticipant = GameParticipant.createPlayer(
                UUID.randomUUID().toString(),
                savedGame.getId(),
                command.hostId()
        );
        participantRepository.save(hostParticipant);

        return new CreateGameResult(
                savedGame.getId(),
                savedGame.getCode(),
                savedGame.getHostId(),
                savedGame.getCurrentDrinkPrice(),
                savedGame.getCurrentMode(),
                savedGame.getMaxPlayers()
        );
    }

    private String generateUniqueCode() {
        for (int i = 0; i < 100; i++) {
            StringBuilder sb = new StringBuilder(CODE_LENGTH);
            for (int j = 0; j < CODE_LENGTH; j++) {
                sb.append(CODE_CHARS.charAt(random.nextInt(CODE_CHARS.length())));
            }
            String code = sb.toString();
            if (!gameRepository.existsByCode(code)) {
                return code;
            }
        }
        return UUID.randomUUID().toString().substring(0, CODE_LENGTH).toUpperCase();
    }
}
