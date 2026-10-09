package com.tappazo.domain.model;

import com.tappazo.domain.exception.DomainException;
import com.tappazo.domain.exception.InvalidVoteException;

import java.time.Instant;
import java.util.*;

/**
 * Entidad de dominio Reveal (Sección 24, 25, 60).
 * Maneja los intentos de revelación de un jugador y su estado de aprobación o invalidez.
 */
public class Reveal {
    public static final int MAX_REVEAL_ATTEMPTS = 3;

    private final String id;
    private final String roundId;
    private final String playerId;
    private RevealStatus status;
    private Integer officialNumber;
    private Instant approvedAt;
    private final List<RevealAttempt> attempts;

    public Reveal(String id, String roundId, String playerId) {
        this.id = Objects.requireNonNull(id, "id must not be null");
        this.roundId = Objects.requireNonNull(roundId, "roundId must not be null");
        this.playerId = Objects.requireNonNull(playerId, "playerId must not be null");
        this.status = RevealStatus.PENDING;
        this.attempts = new ArrayList<>();
    }

    public Reveal(String id, String roundId, String playerId, RevealStatus status,
                  Integer officialNumber, Instant approvedAt, List<RevealAttempt> attempts) {
        this.id = Objects.requireNonNull(id, "id must not be null");
        this.roundId = Objects.requireNonNull(roundId, "roundId must not be null");
        this.playerId = Objects.requireNonNull(playerId, "playerId must not be null");
        this.status = Objects.requireNonNull(status, "status must not be null");
        this.officialNumber = officialNumber;
        this.approvedAt = approvedAt;
        this.attempts = attempts != null ? new ArrayList<>(attempts) : new ArrayList<>();
    }

    /**
     * Agrega un nuevo intento si no se ha alcanzado el límite de 3.
     */
    public RevealAttempt createAttempt(String attemptId, int proposedNumber, String photoReference, String photoOmittedReason) {
        if (this.status != RevealStatus.PENDING) {
            throw new DomainException("No se pueden agregar intentos a una revelación ya resuelta");
        }
        if (this.attempts.size() >= MAX_REVEAL_ATTEMPTS) {
            throw new DomainException("Se alcanzó el número máximo de intentos (" + MAX_REVEAL_ATTEMPTS + ")");
        }
        int nextAttemptNumber = this.attempts.size() + 1;
        RevealAttempt attempt = new RevealAttempt(attemptId, this.id, nextAttemptNumber, proposedNumber, photoReference, photoOmittedReason);
        this.attempts.add(attempt);
        return attempt;
    }

    /**
     * Registra un voto para el intento actual verificando que el revelador no vote su propia tapa (Sección 22, 60, 73.6).
     */
    public void castVote(String attemptId, String voterId, Decision decision, Set<String> eligibleVoterIds) {
        if (voterId.equals(this.playerId)) {
            throw new InvalidVoteException("El revelador no puede votar su propia tapa");
        }
        RevealAttempt attempt = attempts.stream()
                .filter(a -> a.getId().equals(attemptId))
                .findFirst()
                .orElseThrow(() -> new DomainException("Intento no encontrado: " + attemptId));

        attempt.addVote(new Vote(UUID.randomUUID().toString(), attemptId, voterId, decision));
        AttemptStatus newStatus = attempt.evaluateVotes(eligibleVoterIds);

        if (newStatus == AttemptStatus.APPROVED) {
            approve(attempt.getProposedNumber());
        }
    }

    /**
     * Aprueba la revelación con el número oficial.
     */
    public void approve(int number) {
        this.status = RevealStatus.APPROVED;
        this.officialNumber = number;
        this.approvedAt = Instant.now();
    }

    /**
     * Marca la revelación como inválida tras agotarse los intentos y el rechazo en arbitraje (Sección 25).
     */
    public void markInvalid() {
        this.status = RevealStatus.REVEAL_INVALID;
        this.officialNumber = null;
    }

    /**
     * Indica si se requiere arbitraje del host (3er intento rechazado y aún PENDING) (Sección 24, 25).
     */
    public boolean requiresHostArbitration() {
        if (this.status != RevealStatus.PENDING) {
            return false;
        }
        if (attempts.size() == MAX_REVEAL_ATTEMPTS) {
            RevealAttempt lastAttempt = attempts.get(MAX_REVEAL_ATTEMPTS - 1);
            return lastAttempt.getStatus() == AttemptStatus.REJECTED;
        }
        return false;
    }

    public String getId() {
        return id;
    }

    public String getRoundId() {
        return roundId;
    }

    public String getPlayerId() {
        return playerId;
    }

    public RevealStatus getStatus() {
        return status;
    }

    public Integer getOfficialNumber() {
        return officialNumber;
    }

    public Instant getApprovedAt() {
        return approvedAt;
    }

    public List<RevealAttempt> getAttempts() {
        return Collections.unmodifiableList(attempts);
    }
}
