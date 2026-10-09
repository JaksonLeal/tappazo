package com.tappazo.domain.model;

import com.tappazo.domain.exception.InvalidVoteException;

import java.time.Instant;
import java.util.*;

/**
 * Entidad de dominio RevealAttempt (Sección 22, 23, 24, 60).
 * Registra cada intento de revelación con su foto/número y votos individuales asociados.
 */
public class RevealAttempt {
    private final String id;
    private final String revealId;
    private final int attemptNumber;
    private final int proposedNumber;
    private final String photoReference;
    private final String photoOmittedReason;
    private AttemptStatus status;
    private final List<Vote> votes;
    private final Instant createdAt;

    public RevealAttempt(String id, String revealId, int attemptNumber, int proposedNumber,
                         String photoReference, String photoOmittedReason) {
        this.id = Objects.requireNonNull(id, "id must not be null");
        this.revealId = Objects.requireNonNull(revealId, "revealId must not be null");
        if (attemptNumber < 1 || attemptNumber > 3) {
            throw new IllegalArgumentException("attemptNumber debe estar entre 1 y 3");
        }
        this.attemptNumber = attemptNumber;
        if (proposedNumber < 0 || proposedNumber > 99) {
            throw new IllegalArgumentException("El número propuesto debe estar entre 0 y 99");
        }
        this.proposedNumber = proposedNumber;
        this.photoReference = photoReference;
        this.photoOmittedReason = photoOmittedReason;
        this.status = AttemptStatus.PENDING;
        this.votes = new ArrayList<>();
        this.createdAt = Instant.now();
    }

    public RevealAttempt(String id, String revealId, int attemptNumber, int proposedNumber,
                         String photoReference, String photoOmittedReason, AttemptStatus status,
                         List<Vote> votes, Instant createdAt) {
        this.id = Objects.requireNonNull(id, "id must not be null");
        this.revealId = Objects.requireNonNull(revealId, "revealId must not be null");
        this.attemptNumber = attemptNumber;
        this.proposedNumber = proposedNumber;
        this.photoReference = photoReference;
        this.photoOmittedReason = photoOmittedReason;
        this.status = Objects.requireNonNull(status, "status must not be null");
        this.votes = votes != null ? new ArrayList<>(votes) : new ArrayList<>();
        this.createdAt = createdAt != null ? createdAt : Instant.now();
    }

    /**
     * Agrega un voto asegurando unicidad del votante y que no esté ya resuelto.
     */
    public synchronized void addVote(Vote vote) {
        if (this.status != AttemptStatus.PENDING) {
            throw new InvalidVoteException("No se pueden registrar votos en un intento ya resuelto");
        }
        boolean alreadyVoted = votes.stream().anyMatch(v -> v.getVoterId().equals(vote.getVoterId()));
        if (alreadyVoted) {
            throw new InvalidVoteException("El usuario ya votó en este intento");
        }
        this.votes.add(vote);
    }

    /**
     * Evalúa el estado del intento basado en los votos recibidos y los votantes elegibles (Sección 22, 23).
     * Requiere unanimidad entre votantes elegibles activos. Un solo NO produce REJECTED.
     */
    public synchronized AttemptStatus evaluateVotes(Set<String> eligibleVoterIds) {
        if (this.status != AttemptStatus.PENDING) {
            return this.status;
        }

        // Si algún voto registrado (incluso si fue antes de que alguien se desconectara) es NO -> REJECTED
        boolean hasNo = votes.stream().anyMatch(v -> v.getDecision() == Decision.NO);
        if (hasNo) {
            this.status = AttemptStatus.REJECTED;
            return this.status;
        }

        // Para APPROVED se requiere unanimidad de todos los votantes elegibles actuales (Sección 22)
        if (eligibleVoterIds.isEmpty()) {
            this.status = AttemptStatus.APPROVED;
            return this.status;
        }

        Set<String> yesVoters = new HashSet<>();
        for (Vote v : votes) {
            if (v.getDecision() == Decision.YES && eligibleVoterIds.contains(v.getVoterId())) {
                yesVoters.add(v.getVoterId());
            }
        }

        if (yesVoters.containsAll(eligibleVoterIds)) {
            this.status = AttemptStatus.APPROVED;
        }

        return this.status;
    }

    public void markApproved() {
        this.status = AttemptStatus.APPROVED;
    }

    public void markRejected() {
        this.status = AttemptStatus.REJECTED;
    }

    public String getId() {
        return id;
    }

    public String getRevealId() {
        return revealId;
    }

    public int getAttemptNumber() {
        return attemptNumber;
    }

    public int getProposedNumber() {
        return proposedNumber;
    }

    public String getPhotoReference() {
        return photoReference;
    }

    public String getPhotoOmittedReason() {
        return photoOmittedReason;
    }

    public AttemptStatus getStatus() {
        return status;
    }

    public List<Vote> getVotes() {
        return Collections.unmodifiableList(votes);
    }

    public Instant getCreatedAt() {
        return createdAt;
    }
}
