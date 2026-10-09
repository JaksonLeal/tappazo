package com.tappazo.application.usecase;

import com.tappazo.application.port.out.GameNotificationPort;
import com.tappazo.application.port.out.RevealRepositoryPort;
import com.tappazo.application.port.out.RoundParticipantRepositoryPort;
import com.tappazo.application.port.out.RoundRepositoryPort;
import com.tappazo.domain.exception.DomainException;
import com.tappazo.domain.exception.InvalidGameStateException;
import com.tappazo.domain.model.*;

import java.util.Objects;
import java.util.UUID;

/**
 * Caso de uso: Enviar Intento de Revelación (Sección 19, 21, 24, 60).
 * Registra un intento de revelación (foto + número) y habilita la votación paralela.
 */
public class SubmitAttemptUseCase {

    private final RoundRepositoryPort roundRepository;
    private final RoundParticipantRepositoryPort roundParticipantRepository;
    private final RevealRepositoryPort revealRepository;
    private final GameNotificationPort notificationPort;

    public SubmitAttemptUseCase(RoundRepositoryPort roundRepository,
                                RoundParticipantRepositoryPort roundParticipantRepository,
                                RevealRepositoryPort revealRepository,
                                GameNotificationPort notificationPort) {
        this.roundRepository = Objects.requireNonNull(roundRepository, "roundRepository must not be null");
        this.roundParticipantRepository = Objects.requireNonNull(roundParticipantRepository, "roundParticipantRepository must not be null");
        this.revealRepository = Objects.requireNonNull(revealRepository, "revealRepository must not be null");
        this.notificationPort = Objects.requireNonNull(notificationPort, "notificationPort must not be null");
    }

    public record SubmitAttemptCommand(
            String roundId,
            String playerId,
            int proposedNumber,
            String photoReference,
            String photoOmittedReason
    ) {}

    public record SubmitAttemptResult(
            String revealId,
            String attemptId,
            int attemptNumber,
            int proposedNumber,
            AttemptStatus attemptStatus,
            RevealStatus revealStatus
    ) {}

    public SubmitAttemptResult execute(SubmitAttemptCommand command) {
        Objects.requireNonNull(command, "command must not be null");

        Round round = roundRepository.findById(command.roundId())
                .orElseThrow(() -> new DomainException("Ronda no encontrada: " + command.roundId()));

        if (round.getState() != RoundState.REVEALING && round.getState() != RoundState.VALIDATING) {
            throw new InvalidGameStateException("La ronda no está en fase de revelación o validación");
        }

        RoundParticipant participant = roundParticipantRepository.findByRoundIdAndUserId(round.getId(), command.playerId())
                .orElseThrow(() -> new DomainException("El jugador no es participante activo de esta ronda: " + command.playerId()));

        if (participant.getState() != ParticipantState.ACTIVE) {
            throw new DomainException("Solo los jugadores en estado ACTIVE pueden revelar tapas");
        }

        Reveal reveal = revealRepository.findByRoundIdAndPlayerId(round.getId(), command.playerId())
                .orElseGet(() -> {
                    Reveal newReveal = new Reveal(UUID.randomUUID().toString(), round.getId(), command.playerId());
                    return revealRepository.save(newReveal);
                });

        String attemptId = UUID.randomUUID().toString();
        RevealAttempt attempt = reveal.createAttempt(
                attemptId,
                command.proposedNumber(),
                command.photoReference(),
                command.photoOmittedReason()
        );

        if (round.getState() == RoundState.STARTING || round.getState() == RoundState.REVEALING) {
            round.startValidating();
            roundRepository.save(round);
        }

        revealRepository.save(reveal);
        notificationPort.notifyAttemptSubmitted(round.getGameId(), round.getId(), attempt);

        return new SubmitAttemptResult(
                reveal.getId(),
                attempt.getId(),
                attempt.getAttemptNumber(),
                attempt.getProposedNumber(),
                attempt.getStatus(),
                reveal.getStatus()
        );
    }
}
