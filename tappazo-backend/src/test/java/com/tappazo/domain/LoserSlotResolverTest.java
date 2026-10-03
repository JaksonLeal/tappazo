package com.tappazo.domain;

import com.tappazo.domain.model.DebtReason;
import com.tappazo.domain.model.GameResult;
import com.tappazo.domain.model.PlayerResult;
import com.tappazo.domain.service.LoserSlotResolver;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

@DisplayName("Pruebas del Motor de Loser Slots (LoserSlotResolver)")
class LoserSlotResolverTest {

    private LoserSlotResolver resolver;

    @BeforeEach
    void setUp() {
        resolver = new LoserSlotResolver();
    }

    @Test
    @DisplayName("Loser slots críticos: TOTAL=3, [10, 20, 20, 20] -> 1 fijo + 2 de 3 compiten. NUNCA 4 perdedores (Sección 31, 72)")
    void testCriticalLoserSlotsTie() {
        List<PlayerResult> results = List.of(
                PlayerResult.normal("P1", 10),
                PlayerResult.normal("P2", 20),
                PlayerResult.normal("P3", 20),
                PlayerResult.normal("P4", 20)
        );

        GameResult result = resolver.resolve(results, 3);

        assertEquals(3, result.getTotalLoserSlots());
        assertEquals(1, result.getConfirmedLosers().size());
        assertEquals("P1", result.getConfirmedLosers().get(0));
        assertTrue(result.hasTie());
        assertEquals(3, result.getTiedPlayers().size());
        assertTrue(result.getTiedPlayers().containsAll(List.of("P2", "P3", "P4")));
        assertEquals(2, result.getRemainingLoserSlots());
        // El número total de perdedores potenciales nunca superará 3
        assertEquals(3, result.getConfirmedLosers().size() + result.getRemainingLoserSlots());
    }

    @Test
    @DisplayName("Puestos ya llenos sin desempate: 3 loser slots, resultados 10, 20, 20 (Sección 43)")
    void testSlotsAlreadyFullNoTieBreakNeeded() {
        List<PlayerResult> results = List.of(
                PlayerResult.normal("P1", 10),
                PlayerResult.normal("P2", 20),
                PlayerResult.normal("P3", 20)
        );

        GameResult result = resolver.resolve(results, 3);

        assertEquals(3, result.getTotalLoserSlots());
        assertEquals(3, result.getConfirmedLosers().size());
        assertFalse(result.hasTie());
        assertTrue(result.getConfirmedLosers().containsAll(List.of("P1", "P2", "P3")));
        assertEquals(0, result.getRemainingLoserSlots());
        assertTrue(result.getWinners().isEmpty());
    }

    @Test
    @DisplayName("Remociones vs. loser slots: TOTAL=1 (ULTIMO_PIERDE), A y B se desconectan y se remueven -> solo el primero ocupa el loser slot (Sección 30, 72)")
    void testRemovalsVsLoserSlotsSingleSlot() {
        List<PlayerResult> results = List.of(
                PlayerResult.automatic("A", DebtReason.STAND_BY_REMOVAL),
                PlayerResult.automatic("B", DebtReason.STAND_BY_REMOVAL),
                PlayerResult.normal("C", 10),
                PlayerResult.normal("D", 20)
        );

        GameResult result = resolver.resolve(results, 1);

        assertEquals(1, result.getTotalLoserSlots());
        assertEquals(1, result.getConfirmedLosers().size());
        assertEquals("A", result.getConfirmedLosers().get(0)); // Solo el primero ocupa el slot

        // El segundo (B) queda en unassignedAutomaticCandidates, no recibe loser slot ni genera deuda adicional
        assertEquals(1, result.getUnassignedAutomaticCandidates().size());
        assertEquals("B", result.getUnassignedAutomaticCandidates().get(0));

        // Los normales son ganadores porque ya no quedan loser slots
        assertEquals(2, result.getWinners().size());
        assertTrue(result.getWinners().containsAll(List.of("C", "D")));
        assertFalse(result.hasTie());
    }

    @Test
    @DisplayName("Remociones vs. loser slots: TOTAL=2, 3 desconectados -> solo los primeros 2 ocupan slot (Sección 30)")
    void testRemovalsVsLoserSlotsMultipleSlots() {
        List<PlayerResult> results = List.of(
                PlayerResult.automatic("A", DebtReason.STAND_BY_REMOVAL),
                PlayerResult.automatic("B", DebtReason.REVEAL_INVALID),
                PlayerResult.automatic("C", DebtReason.STAND_BY_REMOVAL),
                PlayerResult.normal("D", 15),
                PlayerResult.normal("E", 35)
        );

        GameResult result = resolver.resolve(results, 2);

        assertEquals(2, result.getTotalLoserSlots());
        assertEquals(2, result.getConfirmedLosers().size());
        assertTrue(result.getConfirmedLosers().containsAll(List.of("A", "B")));
        assertEquals(1, result.getUnassignedAutomaticCandidates().size());
        assertEquals("C", result.getUnassignedAutomaticCandidates().get(0));

        assertEquals(2, result.getWinners().size());
        assertTrue(result.getWinners().containsAll(List.of("D", "E")));
        assertFalse(result.hasTie());
    }

    @Test
    @DisplayName("Caso todos empatados: 4 jugadores con mismo número en ULTIMO_PIERDE (Sección 33)")
    void testAllTiedCaseDetected() {
        List<PlayerResult> results = List.of(
                PlayerResult.normal("P1", 20),
                PlayerResult.normal("P2", 20),
                PlayerResult.normal("P3", 20),
                PlayerResult.normal("P4", 20)
        );

        GameResult result = resolver.resolve(results, 1);

        assertTrue(result.hasTie());
        assertTrue(result.isAllTied());
        assertEquals(4, result.getTiedPlayers().size());
        assertEquals(1, result.getRemainingLoserSlots());
        assertTrue(result.getConfirmedLosers().isEmpty());
        assertTrue(result.getWinners().isEmpty());
    }
}
