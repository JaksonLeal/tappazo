package com.tappazo.infrastructure.web.controllers;

import com.tappazo.application.port.out.GameParticipantRepositoryPort;
import com.tappazo.application.port.out.GameRepositoryPort;
import com.tappazo.application.port.out.RoundRepositoryPort;
import com.tappazo.application.port.out.VoiceProviderPort;
import com.tappazo.application.usecase.CreateGameUseCase;
import com.tappazo.application.usecase.CreateGameUseCase.CreateGameCommand;
import com.tappazo.application.usecase.JoinGameUseCase;
import com.tappazo.application.usecase.JoinGameUseCase.JoinGameCommand;
import com.tappazo.domain.exception.DomainException;
import com.tappazo.domain.model.Game;
import com.tappazo.domain.model.GameParticipant;
import com.tappazo.domain.model.Round;
import com.tappazo.infrastructure.security.UserPrincipal;
import com.tappazo.infrastructure.web.dto.WebDTOs.*;
import com.tappazo.infrastructure.web.mapper.WebMappers;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Objects;
import java.util.Optional;

@RestController
@RequestMapping("/api/v1/games")
public class GameController {

    private final CreateGameUseCase createGameUseCase;
    private final JoinGameUseCase joinGameUseCase;
    private final GameRepositoryPort gameRepository;
    private final GameParticipantRepositoryPort participantRepository;
    private final RoundRepositoryPort roundRepository;
    private final VoiceProviderPort voiceProviderPort;

    public GameController(
            CreateGameUseCase createGameUseCase,
            JoinGameUseCase joinGameUseCase,
            GameRepositoryPort gameRepository,
            GameParticipantRepositoryPort participantRepository,
            RoundRepositoryPort roundRepository) {
        this(createGameUseCase, joinGameUseCase, gameRepository, participantRepository, roundRepository, null);
    }

    @org.springframework.beans.factory.annotation.Autowired
    public GameController(
            CreateGameUseCase createGameUseCase,
            JoinGameUseCase joinGameUseCase,
            GameRepositoryPort gameRepository,
            GameParticipantRepositoryPort participantRepository,
            RoundRepositoryPort roundRepository,
            VoiceProviderPort voiceProviderPort) {
        this.createGameUseCase = Objects.requireNonNull(createGameUseCase);
        this.joinGameUseCase = Objects.requireNonNull(joinGameUseCase);
        this.gameRepository = Objects.requireNonNull(gameRepository);
        this.participantRepository = Objects.requireNonNull(participantRepository);
        this.roundRepository = Objects.requireNonNull(roundRepository);
        this.voiceProviderPort = voiceProviderPort;
    }

    @PostMapping
    public ResponseEntity<GameResponse> createGame(
            @Valid @RequestBody CreateGameRequest request,
            @AuthenticationPrincipal UserPrincipal principal) {
        if (principal == null) {
            throw new AccessDeniedException("No autenticado");
        }

        var command = new CreateGameCommand(
                principal.getId(),
                request.drinkPrice(),
                request.initialMode(),
                request.maxPlayers()
        );

        var result = createGameUseCase.execute(command);
        Game game = gameRepository.findById(result.gameId())
                .orElseThrow(() -> new DomainException("Partida no encontrada tras su creación"));

        return ResponseEntity.status(HttpStatus.CREATED).body(WebMappers.toGameResponse(game));
    }

    @PostMapping("/join")
    public ResponseEntity<GameParticipantResponse> joinGame(
            @Valid @RequestBody JoinGameRequest request,
            @AuthenticationPrincipal UserPrincipal principal) {
        if (principal == null) {
            throw new AccessDeniedException("No autenticado");
        }

        var command = new JoinGameCommand(request.gameCode(), principal.getId());
        var result = joinGameUseCase.execute(command);

        GameParticipant participant = participantRepository.findByGameIdAndUserId(result.gameId(), principal.getId())
                .orElseThrow(() -> new DomainException("Error al registrar participante en la partida"));

        return ResponseEntity.ok(WebMappers.toParticipantResponse(participant));
    }

    @GetMapping("/{id}")
    public ResponseEntity<GameDetailsResponse> getGameDetails(@PathVariable String id) {
        Game game = gameRepository.findById(id)
                .orElseThrow(() -> new DomainException("Partida no encontrada con ID: " + id));

        List<GameParticipantResponse> participants = participantRepository.findByGameId(id).stream()
                .map(WebMappers::toParticipantResponse)
                .toList();

        RoundResponse currentRound = roundRepository.findCurrentRoundByGameId(id)
                .map(WebMappers::toRoundResponse)
                .orElse(null);

        return ResponseEntity.ok(new GameDetailsResponse(
                WebMappers.toGameResponse(game),
                participants,
                currentRound
        ));
    }

    @GetMapping("/code/{code}")
    public ResponseEntity<GameResponse> getGameByCode(@PathVariable String code) {
        Game game = gameRepository.findByCode(code.trim().toUpperCase())
                .orElseThrow(() -> new DomainException("Partida no encontrada con código: " + code));

        return ResponseEntity.ok(WebMappers.toGameResponse(game));
    }

    @GetMapping("/{id}/participants")
    public ResponseEntity<List<GameParticipantResponse>> getParticipants(@PathVariable String id) {
        if (!gameRepository.findById(id).isPresent()) {
            throw new DomainException("Partida no encontrada con ID: " + id);
        }

        List<GameParticipantResponse> list = participantRepository.findByGameId(id).stream()
                .map(WebMappers::toParticipantResponse)
                .toList();

        return ResponseEntity.ok(list);
    }

    @PatchMapping("/{id}/settings")
    public ResponseEntity<GameResponse> updateSettings(
            @PathVariable String id,
            @Valid @RequestBody UpdateGameSettingsRequest request,
            @AuthenticationPrincipal UserPrincipal principal) {
        if (principal == null) {
            throw new AccessDeniedException("No autenticado");
        }

        Game game = gameRepository.findById(id)
                .orElseThrow(() -> new DomainException("Partida no encontrada con ID: " + id));

        if (!game.getHostId().equals(principal.getId())) {
            throw new AccessDeniedException("Solo el host de la partida puede modificar los ajustes");
        }

        if (request.drinkPrice() != null) {
            game.updateDrinkPrice(request.drinkPrice());
        }
        if (request.mode() != null) {
            game.changeMode(request.mode());
        }

        Game saved = gameRepository.save(game);
        return ResponseEntity.ok(WebMappers.toGameResponse(saved));
    }

    @PostMapping("/{id}/voice-token")
    public ResponseEntity<VoiceTokenResponse> getVoiceToken(
            @PathVariable String id,
            @AuthenticationPrincipal UserPrincipal principal) {
        if (principal == null) {
            throw new AccessDeniedException("No autenticado");
        }

        Game game = gameRepository.findById(id)
                .orElseThrow(() -> new DomainException("Partida no encontrada con ID: " + id));

        participantRepository.findByGameIdAndUserId(id, principal.getId())
                .orElseThrow(() -> new AccessDeniedException("El usuario no es participante de la partida"));

        if (voiceProviderPort == null) {
            throw new IllegalStateException("Servicio de voz no configurado en el servidor");
        }

        var token = voiceProviderPort.generateToken(id, principal.getId());
        return ResponseEntity.ok(new VoiceTokenResponse(token.token(), token.roomName(), token.serverUrl()));
    }
}
