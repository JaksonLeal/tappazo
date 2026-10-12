package com.tappazo.infrastructure.web.controllers;

import com.tappazo.application.port.out.*;
import com.tappazo.application.usecase.ProcessTieBreakVoteUseCase;
import com.tappazo.application.usecase.ResolveRoundUseCase;
import com.tappazo.domain.exception.DomainException;
import com.tappazo.domain.model.*;
import com.tappazo.infrastructure.security.UserPrincipal;
import com.tappazo.infrastructure.web.dto.WebDTOs.DebtMovementResponse;
import com.tappazo.infrastructure.web.mapper.WebMappers;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import org.springframework.context.annotation.Profile;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import java.util.*;

@RestController
@RequestMapping("/api/v1/local/simulator")
@Profile("local-auth")
@Validated
public class LocalRoundSimulatorController {

    private final GameRepositoryPort gameRepository;
    private final GameParticipantRepositoryPort gameParticipantRepository;
    private final RoundRepositoryPort roundRepository;
    private final RoundParticipantRepositoryPort roundParticipantRepository;
    private final RevealRepositoryPort revealRepository;
    private final TieBreakRepositoryPort tieBreakRepository;
    private final ResolveRoundUseCase resolveRoundUseCase;
    private final ProcessTieBreakVoteUseCase processTieBreakVoteUseCase;

    public LocalRoundSimulatorController(
            GameRepositoryPort gameRepository,
            GameParticipantRepositoryPort gameParticipantRepository,
            RoundRepositoryPort roundRepository,
            RoundParticipantRepositoryPort roundParticipantRepository,
            RevealRepositoryPort revealRepository,
            TieBreakRepositoryPort tieBreakRepository,
            ResolveRoundUseCase resolveRoundUseCase,
            ProcessTieBreakVoteUseCase processTieBreakVoteUseCase) {
        this.gameRepository = Objects.requireNonNull(gameRepository);
        this.gameParticipantRepository = Objects.requireNonNull(gameParticipantRepository);
        this.roundRepository = Objects.requireNonNull(roundRepository);
        this.roundParticipantRepository = Objects.requireNonNull(roundParticipantRepository);
        this.revealRepository = Objects.requireNonNull(revealRepository);
        this.tieBreakRepository = Objects.requireNonNull(tieBreakRepository);
        this.resolveRoundUseCase = Objects.requireNonNull(resolveRoundUseCase);
        this.processTieBreakVoteUseCase = Objects.requireNonNull(processTieBreakVoteUseCase);
    }

    public record PlayerNumber(
            @NotBlank String userId,
            @Min(0) @Max(99) int number
    ) {}

    public record SimulateRoundRequest(@NotEmpty List<@Valid PlayerNumber> results) {}

    public record SimulatedRoundResponse(
            String gameId,
            String roundId,
            String roundState,
            boolean hasTie,
            String tieBreakId,
            List<String> confirmedLosers,
            List<String> winners,
            List<DebtMovementResponse> debtMovements
    ) {}

    public record SimulatedTieBreakVote(
            @NotBlank String playerId,
            @NotNull TieBreakResolutionMode decision
    ) {}

    public record ResolveTieBreakRequest(
            List<@Valid SimulatedTieBreakVote> votes,
            TieBreakResolutionMode deciderDecision
    ) {}

    public record SimulatedTieBreakResponse(
            String tieBreakId,
            String status,
            String resolutionMode,
            String selectedDeciderId,
            String tieBreakRoundId,
            List<DebtMovementResponse> debtMovements
    ) {}

    @PostMapping("/games/{gameId}/rounds")
    @Transactional
    public ResponseEntity<SimulatedRoundResponse> simulateRound(
            @PathVariable String gameId,
            @Valid @RequestBody SimulateRoundRequest request,
            @AuthenticationPrincipal UserPrincipal principal) {
        Game game = requireHostGame(gameId, principal);
            boolean continuingAllTiedRound = game.getState() == GameState.IN_PROGRESS;
            if (!continuingAllTiedRound && game.getState() != GameState.LOBBY) {
                throw new DomainException("La partida debe estar en LOBBY o esperando la ronda de un empate total");
            }

            List<GameParticipant> players = gameParticipantRepository.findByGameId(gameId).stream()
                .filter(GameParticipant::isPlayer)
                .filter(GameParticipant::isActive)
                .sorted(Comparator.comparing(GameParticipant::getUserId))
                .toList();
        if (players.size() < game.getCurrentMode().getMinPlayers()) {
            throw new DomainException(
                    "La modalidad " + game.getCurrentMode() + " requiere al menos "
                            + game.getCurrentMode().getMinPlayers() + " jugadores activos"
            );
        }
        if (players.size() > game.getMaxPlayers()) {
            throw new DomainException("La cantidad de jugadores activos supera el máximo configurado");
        }

        Round round;
        List<RoundParticipant> roundParticipants;
        if (continuingAllTiedRound) {
            round = findAllTiedFollowUpRound(gameId);
            roundParticipants = roundParticipantRepository.findByRoundId(round.getId());
            Set<String> roundPlayerIds = new HashSet<>();
            roundParticipants.forEach(participant -> roundPlayerIds.add(participant.getUserId()));
            Set<String> activePlayerIds = new HashSet<>();
            players.forEach(player -> activePlayerIds.add(player.getUserId()));
            if (!roundPlayerIds.equals(activePlayerIds)) {
                throw new DomainException(
                        "El simulador local solo admite continuar desempates totales con todos los jugadores activos"
                );
            }
        } else {
            String roundId = UUID.randomUUID().toString();
            round = new Round(
                    roundId,
                    gameId,
                    game.getCurrentMode(),
                    game.getCurrentDrinkPrice(),
                    RoundType.NORMAL
            );
            roundParticipants = new ArrayList<>();
            int order = 1;
            for (GameParticipant player : players) {
                roundParticipants.add(new RoundParticipant(
                        UUID.randomUUID().toString(),
                        roundId,
                        player.getUserId(),
                        player.getUserId(),
                        order++
                ));
            }
        }

        Map<String, Integer> numbersByPlayer = new HashMap<>();
        for (PlayerNumber result : request.results()) {
            if (numbersByPlayer.putIfAbsent(result.userId(), result.number()) != null) {
                throw new DomainException("Hay más de un resultado para el jugador " + result.userId());
            }
        }
        Set<String> expectedPlayerIds = new HashSet<>();
        roundParticipants.forEach(participant -> expectedPlayerIds.add(participant.getUserId()));
        if (!numbersByPlayer.keySet().equals(expectedPlayerIds)) {
            throw new DomainException("Debes enviar exactamente un número para cada jugador de la ronda");
        }

        round.startRevealing();
        round.startValidating();
        roundRepository.save(round);
        if (!continuingAllTiedRound) {
            game.startRound();
            gameRepository.save(game);
            roundParticipantRepository.saveAll(roundParticipants);
        }

        for (RoundParticipant participant : roundParticipants) {
            Reveal reveal = new Reveal(
                    UUID.randomUUID().toString(),
                    round.getId(),
                    participant.getUserId()
            );
            reveal.approve(numbersByPlayer.get(participant.getUserId()));
            revealRepository.save(reveal);
        }

        ResolveRoundUseCase.ResolveRoundResult result = resolveRoundUseCase.execute(
                new ResolveRoundUseCase.ResolveRoundCommand(round.getId())
        );
        SimulatedRoundResponse response = new SimulatedRoundResponse(
                gameId,
                result.roundId(),
                result.roundState().name(),
                result.hasTie(),
                result.tieBreakId(),
                result.confirmedLosers(),
                result.winners(),
                result.debtMovements().stream().map(WebMappers::toDebtMovementResponse).toList()
        );
        return ResponseEntity.status(continuingAllTiedRound ? HttpStatus.OK : HttpStatus.CREATED).body(response);
    }

    @PostMapping("/tie-breaks/{tieBreakId}/resolve")
    @Transactional
    public ResponseEntity<SimulatedTieBreakResponse> resolveTieBreak(
            @PathVariable String tieBreakId,
            @RequestBody(required = false) @Valid ResolveTieBreakRequest request,
            @AuthenticationPrincipal UserPrincipal principal) {
        TieBreak tieBreak = tieBreakRepository.findById(tieBreakId)
                .orElseThrow(() -> new DomainException("Desempate no encontrado: " + tieBreakId));
        Round sourceRound = roundRepository.findById(tieBreak.getSourceRoundId())
                .orElseThrow(() -> new DomainException("Ronda origen no encontrada"));
        requireHostGame(sourceRound.getGameId(), principal);

        List<SimulatedTieBreakVote> votes = request != null && request.votes() != null
                ? request.votes()
                : List.of();
        Set<String> submittedVoters = new HashSet<>();
        Set<String> tieParticipantIds = new HashSet<>();
        tieBreak.getParticipants().forEach(participant -> tieParticipantIds.add(participant.getPlayerId()));
        for (SimulatedTieBreakVote vote : votes) {
            if (!submittedVoters.add(vote.playerId())) {
                throw new DomainException("No repitas el voto del jugador " + vote.playerId());
            }
            if (!tieParticipantIds.contains(vote.playerId())) {
                throw new DomainException("El jugador " + vote.playerId() + " no está empatado en este desempate");
            }
        }

        ProcessTieBreakVoteUseCase.ProcessTieBreakVoteResult result = null;
        for (SimulatedTieBreakVote vote : votes) {
            if (tieBreakRepository.findById(tieBreakId).orElseThrow().getStatus() == TieBreakStatus.RESOLVED) {
                break;
            }
            result = processTieBreakVoteUseCase.execute(
                    new ProcessTieBreakVoteUseCase.ProcessTieBreakVoteCommand(
                            tieBreakId,
                            vote.playerId(),
                            vote.decision()
                    )
            );
        }

        TieBreak latestTieBreak = tieBreakRepository.findById(tieBreakId).orElseThrow();
        if (latestTieBreak.getStatus() == TieBreakStatus.PENDING
                && latestTieBreak.getSelectedDeciderId() != null
                && request != null
                && request.deciderDecision() != null) {
            result = processTieBreakVoteUseCase.execute(
                    new ProcessTieBreakVoteUseCase.ProcessTieBreakVoteCommand(
                            tieBreakId,
                            latestTieBreak.getSelectedDeciderId(),
                            request.deciderDecision()
                    )
            );
        }

        TieBreak updatedTieBreak = tieBreakRepository.findById(tieBreakId).orElseThrow();
        List<DebtMovementResponse> debtMovements = result != null
                ? result.debtMovements().stream().map(WebMappers::toDebtMovementResponse).toList()
                : List.of();
        return ResponseEntity.ok(new SimulatedTieBreakResponse(
                updatedTieBreak.getId(),
                updatedTieBreak.getStatus().name(),
                updatedTieBreak.getResolutionMode() != null ? updatedTieBreak.getResolutionMode().name() : null,
                updatedTieBreak.getSelectedDeciderId(),
                updatedTieBreak.getTieBreakRoundId(),
                debtMovements
        ));
    }

    private Game requireHostGame(String gameId, UserPrincipal principal) {
        if (principal == null) {
            throw new AccessDeniedException("No autenticado");
        }
        Game game = gameRepository.findById(gameId)
                .orElseThrow(() -> new DomainException("Partida no encontrada: " + gameId));
        if (!game.getHostId().equals(principal.getId())) {
            throw new AccessDeniedException("Solo el anfitrión puede utilizar el simulador local");
        }
        return game;
    }

    private Round findAllTiedFollowUpRound(String gameId) {
        List<Round> rounds = roundRepository.findByGameId(gameId);
        for (Round candidate : rounds) {
            if (candidate.getRoundType() != RoundType.TIE_BREAK_ROUND
                    || candidate.getState() != RoundState.STARTING) {
                continue;
            }
            for (Round source : rounds) {
                TieBreak sourceTieBreak = tieBreakRepository.findBySourceRoundId(source.getId()).orElse(null);
                if (sourceTieBreak != null
                        && candidate.getId().equals(sourceTieBreak.getTieBreakRoundId())
                        && sourceTieBreak.getStatus() == TieBreakStatus.RESOLVED
                        && sourceTieBreak.isAllTiedCase()) {
                    return candidate;
                }
            }
        }
        throw new DomainException("No hay una ronda de desempate total pendiente para simular");
    }
}
