package com.tappazo.domain;

import com.tappazo.domain.exception.InvalidVoteException;
import com.tappazo.domain.model.*;
import com.tappazo.domain.policy.TimeoutPolicy;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.*;

@DisplayName("Pruebas de Votación, Intentos y Arbitraje (Sección 22, 23, 24, 25, 72)")
class VotingAndArbitrationTest {

    private Reveal reveal;

    @BeforeEach
    void setUp() {
        reveal = new Reveal("REV-1", "ROUND-1", "Player-A");
    }

    @Test
    @DisplayName("El revelador NO puede votar su propia tapa (Sección 22, 60, 72, 73.6)")
    void testRevealerCannotVoteOwnCap() {
        RevealAttempt attempt = reveal.createAttempt("ATT-1", 17, "photo-url", null);

        assertThrows(InvalidVoteException.class, () ->
                reveal.castVote(attempt.getId(), "Player-A", Decision.YES, Set.of("Player-B", "Player-C"))
        );
    }

    @Test
    @DisplayName("Votación con unanimidad: todos Sí -> APPROVED (Sección 23, 72)")
    void testUnanimousYesApprovesAttempt() {
        RevealAttempt attempt = reveal.createAttempt("ATT-1", 17, "photo-url", null);
        Set<String> eligibleVoters = Set.of("Player-B", "Player-C");

        reveal.castVote(attempt.getId(), "Player-B", Decision.YES, eligibleVoters);
        assertEquals(AttemptStatus.PENDING, attempt.getStatus());
        assertEquals(RevealStatus.PENDING, reveal.getStatus());

        reveal.castVote(attempt.getId(), "Player-C", Decision.YES, eligibleVoters);
        assertEquals(AttemptStatus.APPROVED, attempt.getStatus());
        assertEquals(RevealStatus.APPROVED, reveal.getStatus());
        assertEquals(17, reveal.getOfficialNumber());
    }

    @Test
    @DisplayName("Votación: un solo NO -> REJECTED para ese intento (Sección 23, 72)")
    void testSingleNoRejectsAttempt() {
        RevealAttempt attempt = reveal.createAttempt("ATT-1", 17, "photo-url", null);
        Set<String> eligibleVoters = Set.of("Player-B", "Player-C", "Player-D");

        reveal.castVote(attempt.getId(), "Player-B", Decision.YES, eligibleVoters);
        reveal.castVote(attempt.getId(), "Player-C", Decision.NO, eligibleVoters);

        assertEquals(AttemptStatus.REJECTED, attempt.getStatus());
        assertEquals(RevealStatus.PENDING, reveal.getStatus()); // El Reveal sigue pendiente para otro intento
    }

    @Test
    @DisplayName("3er rechazo conduce a arbitraje del host (Sección 24, 25, 72)")
    void testThirdRejectionRequiresArbitration() {
        // Intento 1 rechazado
        RevealAttempt att1 = reveal.createAttempt("ATT-1", 17, "url-1", null);
        reveal.castVote(att1.getId(), "Player-B", Decision.NO, Set.of("Player-B"));
        assertFalse(reveal.requiresHostArbitration());

        // Intento 2 rechazado
        RevealAttempt att2 = reveal.createAttempt("ATT-2", 18, "url-2", null);
        reveal.castVote(att2.getId(), "Player-B", Decision.NO, Set.of("Player-B"));
        assertFalse(reveal.requiresHostArbitration());

        // Intento 3 rechazado
        RevealAttempt att3 = reveal.createAttempt("ATT-3", 19, "url-3", null);
        reveal.castVote(att3.getId(), "Player-B", Decision.NO, Set.of("Player-B"));

        // Se alcanzaron 3 intentos rechazados -> requiere arbitraje
        assertTrue(reveal.requiresHostArbitration());

        // Árbitro aprueba
        reveal.approve(19);
        assertEquals(RevealStatus.APPROVED, reveal.getStatus());
        assertEquals(19, reveal.getOfficialNumber());
    }

    @Test
    @DisplayName("Arbitraje del host rechaza definitivamente -> REVEAL_INVALID (Sección 25, 30)")
    void testArbitrationRejectionMarksInvalid() {
        reveal.createAttempt("ATT-1", 17, "url-1", null).markRejected();
        reveal.createAttempt("ATT-2", 18, "url-2", null).markRejected();
        reveal.createAttempt("ATT-3", 19, "url-3", null).markRejected();

        assertTrue(reveal.requiresHostArbitration());

        // Árbitro rechaza definitivamente
        reveal.markInvalid();
        assertEquals(RevealStatus.REVEAL_INVALID, reveal.getStatus());
        assertNull(reveal.getOfficialNumber());
    }

    @Test
    @DisplayName("Votación paralela y desconexión: C pasa a STAND_BY y se excluye de la votación, B+D deciden (Sección 22, 72)")
    void testDisconnectionDuringParallelVotingExcludesStandByPlayer() {
        RevealAttempt attempt = reveal.createAttempt("ATT-1", 17, "photo-url", null);

        // Votantes iniciales: B, C, D
        // B vota YES
        reveal.castVote(attempt.getId(), "Player-B", Decision.YES, Set.of("Player-B", "Player-C", "Player-D"));
        assertEquals(AttemptStatus.PENDING, attempt.getStatus());

        // D vota YES
        reveal.castVote(attempt.getId(), "Player-D", Decision.YES, Set.of("Player-B", "Player-C", "Player-D"));
        assertEquals(AttemptStatus.PENDING, attempt.getStatus());

        // C se desconecta y pasa a STAND_BY -> Votantes elegibles activos ahora son solo {B, D}
        Set<String> activeEligibleVoters = Set.of("Player-B", "Player-D");
        AttemptStatus status = attempt.evaluateVotes(activeEligibleVoters);

        assertEquals(AttemptStatus.APPROVED, status);

        // Si C regresa después e intenta votar, la votación ya está resuelta y se rechaza el voto (Sección 16, 24)
        assertThrows(InvalidVoteException.class, () ->
                attempt.addVote(new Vote("V-C", attempt.getId(), "Player-C", Decision.YES))
        );
    }

    @Test
    @DisplayName("TimeoutPolicy: verifica umbrales de 15s de acción y 1 min de stand by (Sección 18)")
    void testTimeoutPolicyThresholds() {
        Instant start = Instant.parse("2026-10-02T10:00:00Z");

        // Menos de 15s
        assertFalse(TimeoutPolicy.isActionTimedOut(start, Instant.parse("2026-10-02T10:00:14Z")));
        // Exactamente o más de 15s
        assertTrue(TimeoutPolicy.isActionTimedOut(start, Instant.parse("2026-10-02T10:00:15Z")));

        // Menos de 1 minuto de standby
        assertFalse(TimeoutPolicy.isStandByWaitTimedOut(start, Instant.parse("2026-10-02T10:00:59Z")));
        // 1 minuto de standby
        assertTrue(TimeoutPolicy.isStandByWaitTimedOut(start, Instant.parse("2026-10-02T10:01:00Z")));

        // Empate standby (30 segundos)
        assertFalse(TimeoutPolicy.isStandByTieWaitTimedOut(start, Instant.parse("2026-10-02T10:00:29Z")));
        assertTrue(TimeoutPolicy.isStandByTieWaitTimedOut(start, Instant.parse("2026-10-02T10:00:30Z")));
    }
}
