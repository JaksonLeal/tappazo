package com.tappazo.domain;

import com.tappazo.domain.exception.InsufficientPlayersException;
import com.tappazo.domain.model.GameMode;
import com.tappazo.domain.model.GameResult;
import com.tappazo.domain.model.PlayerResult;
import com.tappazo.domain.rule.GameModeRuleRegistry;
import com.tappazo.domain.rule.LastLosesRule;
import com.tappazo.domain.rule.LastTwoLoseRule;
import com.tappazo.domain.rule.SmallNumbersPayRule;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

@DisplayName("Pruebas de Modalidades de Juego (GameModeRule)")
class GameModeRuleTest {

    private GameModeRuleRegistry registry;
    private LastLosesRule lastLosesRule;
    private SmallNumbersPayRule smallNumbersPayRule;
    private LastTwoLoseRule lastTwoLoseRule;

    @BeforeEach
    void setUp() {
        registry = new GameModeRuleRegistry();
        lastLosesRule = (LastLosesRule) registry.getRule(GameMode.ULTIMO_PIERDE);
        smallNumbersPayRule = (SmallNumbersPayRule) registry.getRule(GameMode.PEQUENOS_PAGAN);
        lastTwoLoseRule = (LastTwoLoseRule) registry.getRule(GameMode.ULTIMOS_DOS_PIERDEN);
    }

    @Test
    @DisplayName("Validación de mínimos: 2 jugadores en ULTIMO_PIERDE puede iniciar")
    void testUltimoPierdeMinPlayers() {
        assertEquals(2, lastLosesRule.minimoJugadores());
        List<PlayerResult> players = List.of(
                PlayerResult.normal("P1", 20),
                PlayerResult.normal("P2", 30)
        );
        assertDoesNotThrow(() -> lastLosesRule.resolve(players));
    }

    @Test
    @DisplayName("Validación de mínimos: 3 jugadores en ULTIMOS_DOS_PIERDEN NO puede iniciar (mínimo 4)")
    void testUltimosDosPierdenRequiresMinFour() {
        assertEquals(4, lastTwoLoseRule.minimoJugadores());
        List<PlayerResult> threePlayers = List.of(
                PlayerResult.normal("P1", 10),
                PlayerResult.normal("P2", 20),
                PlayerResult.normal("P3", 30)
        );
        assertThrows(InsufficientPlayersException.class, () -> lastTwoLoseRule.resolve(threePlayers));
    }

    @Test
    @DisplayName("Validación de mínimos: 1 jugador nunca puede iniciar en ninguna modalidad")
    void testSinglePlayerCannotStartAnyMode() {
        List<PlayerResult> singlePlayer = List.of(PlayerResult.normal("P1", 10));

        assertThrows(InsufficientPlayersException.class, () -> lastLosesRule.resolve(singlePlayer));
        assertThrows(InsufficientPlayersException.class, () -> smallNumbersPayRule.resolve(singlePlayer));
        assertThrows(InsufficientPlayersException.class, () -> lastTwoLoseRule.resolve(singlePlayer));
    }

    @Test
    @DisplayName("Validación de mínimos: 2 jugadores en PEQUENOS_PAGAN NO puede iniciar (mínimo 3)")
    void testPequenosPaganRequiresMinThree() {
        assertEquals(3, smallNumbersPayRule.minimoJugadores());
        List<PlayerResult> twoPlayers = List.of(
                PlayerResult.normal("P1", 10),
                PlayerResult.normal("P2", 20)
        );
        assertThrows(InsufficientPlayersException.class, () -> smallNumbersPayRule.resolve(twoPlayers));
    }

    @Test
    @DisplayName("Cálculo de perdedores en PEQUENOS_PAGAN: 11 jugadores -> 5 perdedores, 6 ganadores (Sección 40, 41)")
    void testPequenosPaganElevenPlayers() {
        assertEquals(5, smallNumbersPayRule.calculateLoserSlots(11));
        List<PlayerResult> elevenPlayers = List.of(
                PlayerResult.normal("P1", 10),
                PlayerResult.normal("P2", 15),
                PlayerResult.normal("P3", 20),
                PlayerResult.normal("P4", 25),
                PlayerResult.normal("P5", 30),
                PlayerResult.normal("P6", 35),
                PlayerResult.normal("P7", 40),
                PlayerResult.normal("P8", 45),
                PlayerResult.normal("P9", 50),
                PlayerResult.normal("P10", 55),
                PlayerResult.normal("P11", 60)
        );

        GameResult result = smallNumbersPayRule.resolve(elevenPlayers);
        assertEquals(5, result.getTotalLoserSlots());
        assertEquals(5, result.getConfirmedLosers().size());
        assertEquals(6, result.getWinners().size());
        assertFalse(result.hasTie());
        assertTrue(result.getConfirmedLosers().containsAll(List.of("P1", "P2", "P3", "P4", "P5")));
        assertTrue(result.getWinners().containsAll(List.of("P6", "P7", "P8", "P9", "P10", "P11")));
    }

    @Test
    @DisplayName("Último pierde: [25, 17, 25, 25] -> 17 pierde (Sección 72)")
    void testUltimoPierdeSingleLoser() {
        List<PlayerResult> results = List.of(
                PlayerResult.normal("A", 25),
                PlayerResult.normal("B", 17),
                PlayerResult.normal("C", 25),
                PlayerResult.normal("D", 25)
        );

        GameResult result = lastLosesRule.resolve(results);
        assertEquals(1, result.getTotalLoserSlots());
        assertEquals(1, result.getConfirmedLosers().size());
        assertEquals("B", result.getConfirmedLosers().get(0));
        assertEquals(3, result.getWinners().size());
        assertFalse(result.hasTie());
    }

    @Test
    @DisplayName("Últimos 2: [10, 10, 20, 30] -> ambos 10 pierden, sin desempate (Sección 42, 72)")
    void testUltimosDosPierdenNoTie() {
        List<PlayerResult> results = List.of(
                PlayerResult.normal("A", 10),
                PlayerResult.normal("B", 10),
                PlayerResult.normal("C", 20),
                PlayerResult.normal("D", 30)
        );

        GameResult result = lastTwoLoseRule.resolve(results);
        assertEquals(2, result.getTotalLoserSlots());
        assertEquals(2, result.getConfirmedLosers().size());
        assertTrue(result.getConfirmedLosers().containsAll(List.of("A", "B")));
        assertEquals(2, result.getWinners().size());
        assertTrue(result.getWinners().containsAll(List.of("C", "D")));
        assertFalse(result.hasTie());
    }

    @Test
    @DisplayName("Empate en Últimos dos pierden: [10, 20, 20, 30] -> 10 confirmado, los dos 20 compiten por 1 puesto restante (Sección 42, 72)")
    void testUltimosDosPierdenTieAtBoundary() {
        List<PlayerResult> results = List.of(
                PlayerResult.normal("A", 10),
                PlayerResult.normal("B", 20),
                PlayerResult.normal("C", 20),
                PlayerResult.normal("D", 30)
        );

        GameResult result = lastTwoLoseRule.resolve(results);
        assertEquals(2, result.getTotalLoserSlots());
        assertEquals(1, result.getConfirmedLosers().size());
        assertEquals("A", result.getConfirmedLosers().get(0));
        assertTrue(result.hasTie());
        assertEquals(2, result.getTiedPlayers().size());
        assertTrue(result.getTiedPlayers().containsAll(List.of("B", "C")));
        assertEquals(1, result.getRemainingLoserSlots());
        assertEquals(1, result.getWinners().size());
        assertEquals("D", result.getWinners().get(0));
    }
}
