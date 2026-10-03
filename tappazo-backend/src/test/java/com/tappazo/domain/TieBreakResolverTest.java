package com.tappazo.domain;

import com.tappazo.domain.model.GameResult;
import com.tappazo.domain.model.PlayerResult;
import com.tappazo.domain.model.TieBreak;
import com.tappazo.domain.model.TieBreakResolutionMode;
import com.tappazo.domain.service.DeciderSelector;
import com.tappazo.domain.service.LoserPortionCalculator;
import com.tappazo.domain.service.LoserSlotResolver;
import com.tappazo.domain.service.TieBreakResolver;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

@DisplayName("Pruebas del Motor de Desempate (TieBreakResolver)")
class TieBreakResolverTest {

    private TieBreakResolver tieBreakResolver;
    private LoserSlotResolver loserSlotResolver;

    @BeforeEach
    void setUp() {
        loserSlotResolver = new LoserSlotResolver();
        DeciderSelector deterministicSelector = eligible -> eligible.get(0); // Selecciona el primer no-empatado
        tieBreakResolver = new TieBreakResolver(deterministicSelector, new LoserPortionCalculator(), loserSlotResolver);
    }

    @Test
    @DisplayName("Desempate: consenso unánime entre empatados aplica esa opción (Sección 33)")
    void testUnanimousAgreementAppliesOption() {
        // Ronda inicial: P1 pierde (10), P2 y P3 empatan en 20 por 1 slot restante
        List<PlayerResult> initial = List.of(
                PlayerResult.normal("P1", 10),
                PlayerResult.normal("P2", 20),
                PlayerResult.normal("P3", 20),
                PlayerResult.normal("P4", 40)
        );
        GameResult gameResult = loserSlotResolver.resolve(initial, 2);
        assertTrue(gameResult.hasTie());

        TieBreak tieBreak = tieBreakResolver.initiateTieBreak("TB-1", "R1", gameResult);

        // P2 y P3 ambos votan DIVIDIR_CUENTA
        Map<String, TieBreakResolutionMode> votes = Map.of(
                "P2", TieBreakResolutionMode.DIVIDIR_CUENTA,
                "P3", TieBreakResolutionMode.DIVIDIR_CUENTA
        );

        TieBreakResolutionMode resolvedMode = tieBreakResolver.resolveTieBreakMode(
                tieBreak,
                votes,
                List.of("P1", "P4"),
                false
        );

        assertEquals(TieBreakResolutionMode.DIVIDIR_CUENTA, resolvedMode);
        assertEquals(TieBreakResolutionMode.DIVIDIR_CUENTA, tieBreak.getResolutionMode());
        assertNull(tieBreak.getSelectedDeciderId());
    }

    @Test
    @DisplayName("Desempate por desacuerdo: selecciona árbitro no-empatado en backend y este decide (Sección 33, 72)")
    void testDisagreementSelectsDecider() {
        List<PlayerResult> initial = List.of(
                PlayerResult.normal("P1", 10),
                PlayerResult.normal("P2", 20),
                PlayerResult.normal("P3", 20),
                PlayerResult.normal("P4", 40)
        );
        GameResult gameResult = loserSlotResolver.resolve(initial, 2);

        TieBreak tieBreak = tieBreakResolver.initiateTieBreak("TB-1", "R1", gameResult);

        // Desacuerdo: P2 vota NUEVA_RONDA, P3 vota DIVIDIR_CUENTA
        Map<String, TieBreakResolutionMode> votes = Map.of(
                "P2", TieBreakResolutionMode.NUEVA_RONDA,
                "P3", TieBreakResolutionMode.DIVIDIR_CUENTA
        );

        TieBreakResolutionMode resolvedMode = tieBreakResolver.resolveTieBreakMode(
                tieBreak,
                votes,
                List.of("P4"), // P4 es no-empatado elegible
                false
        );

        assertNull(resolvedMode); // Requiere voto del árbitro
        assertEquals("P4", tieBreak.getSelectedDeciderId());

        // P4 decide NUEVA_RONDA
        tieBreakResolver.applyDeciderDecision(tieBreak, "P4", TieBreakResolutionMode.NUEVA_RONDA);
        assertEquals(TieBreakResolutionMode.NUEVA_RONDA, tieBreak.getResolutionMode());
    }

    @Test
    @DisplayName("Caso todos empatados: 1er empate total -> NUEVA_RONDA automática (Sección 33, 72)")
    void testAllTiedCaseFirstTimeTriggersNewRound() {
        List<PlayerResult> initial = List.of(
                PlayerResult.normal("P1", 20),
                PlayerResult.normal("P2", 20),
                PlayerResult.normal("P3", 20)
        );
        GameResult gameResult = loserSlotResolver.resolve(initial, 1);
        assertTrue(gameResult.isAllTied());

        TieBreak tieBreak = tieBreakResolver.initiateTieBreak("TB-ALL", "R1", gameResult);

        TieBreakResolutionMode mode = tieBreakResolver.resolveTieBreakMode(
                tieBreak,
                Map.of(),
                List.of(),
                false // Primer empate total
        );

        assertEquals(TieBreakResolutionMode.NUEVA_RONDA, mode);
        assertEquals(TieBreakResolutionMode.NUEVA_RONDA, tieBreak.getResolutionMode());
    }

    @Test
    @DisplayName("Caso todos empatados: 2do empate total consecutivo -> DIVIDIR_CUENTA forzado (Sección 33, 72)")
    void testAllTiedCaseSecondTimeForcesDividirCuenta() {
        List<PlayerResult> initial = List.of(
                PlayerResult.normal("P1", 20),
                PlayerResult.normal("P2", 20),
                PlayerResult.normal("P3", 20)
        );
        GameResult gameResult = loserSlotResolver.resolve(initial, 1);

        TieBreak tieBreak = tieBreakResolver.initiateTieBreak("TB-ALL-2", "R2", gameResult);

        TieBreakResolutionMode mode = tieBreakResolver.resolveTieBreakMode(
                tieBreak,
                Map.of(),
                List.of(),
                true // Segundo empate total consecutivo
        );

        assertEquals(TieBreakResolutionMode.DIVIDIR_CUENTA, mode);
        assertEquals(TieBreakResolutionMode.DIVIDIR_CUENTA, tieBreak.getResolutionMode());
    }

    @Test
    @DisplayName("Nueva ronda como desempate: solo los competidores compiten, total perdedores es exacto (Sección 34)")
    void testNewRoundResolutionOnlyCompetitorsCompete() {
        // Ronda inicial: 5 jugadores. P1 perdedor confirmado (10), P4 y P5 ganadores confirmados (40, 50).
        // P2 y P3 empatados en 20 compiten por 1 loser slot restante. Total slots = 2.
        List<PlayerResult> initial = List.of(
                PlayerResult.normal("P1", 10),
                PlayerResult.normal("P2", 20),
                PlayerResult.normal("P3", 20),
                PlayerResult.normal("P4", 40),
                PlayerResult.normal("P5", 50)
        );
        GameResult initialResult = loserSlotResolver.resolve(initial, 2);

        assertEquals(1, initialResult.getConfirmedLosers().size());
        assertEquals("P1", initialResult.getConfirmedLosers().get(0));
        assertEquals(2, initialResult.getWinners().size());
        assertEquals(1, initialResult.getRemainingLoserSlots());

        // En la nueva ronda destapan físicamente todos: P1=5, P2=12, P3=88, P4=2, P5=99
        // Aunque P4 sacó 2 y P1 sacó 5, ellos NO compiten por el desempate (Sección 34).
        // Solo compiten P2 y P3: P2 sacó 12 (menor) y P3 sacó 88.
        List<PlayerResult> tieBreakRoundResults = List.of(
                PlayerResult.normal("P1", 5),
                PlayerResult.normal("P2", 12),
                PlayerResult.normal("P3", 88),
                PlayerResult.normal("P4", 2),
                PlayerResult.normal("P5", 99)
        );

        GameResult finalResult = tieBreakResolver.resolveRemainingSlotsFromTieBreakRound(
                initialResult,
                tieBreakRoundResults
        );

        // Total perdedores final = 2 (P1 inicial + P2 del desempate)
        assertEquals(2, finalResult.getTotalLoserSlots());
        assertEquals(2, finalResult.getConfirmedLosers().size());
        assertTrue(finalResult.getConfirmedLosers().containsAll(List.of("P1", "P2")));

        // P3 (perdió el desempate por salir más alto) se une a los ganadores (P4, P5, P3)
        assertEquals(3, finalResult.getWinners().size());
        assertTrue(finalResult.getWinners().containsAll(List.of("P4", "P5", "P3")));

        assertFalse(finalResult.hasTie());
    }
}
