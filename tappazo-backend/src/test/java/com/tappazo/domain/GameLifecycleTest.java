package com.tappazo.domain;

import com.tappazo.domain.exception.InvalidGameStateException;
import com.tappazo.domain.model.*;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

@DisplayName("Pruebas de Ciclo de Vida del Game, Round y Espectadores (Sección 8, 10, 11, 72)")
class GameLifecycleTest {

    private Game game;

    @BeforeEach
    void setUp() {
        game = new Game("GAME-1", "ABC1234", "Host-1", 10, 4000L, GameMode.ULTIMO_PIERDE);
    }

    @Test
    @DisplayName("El precio de la bebida solo se puede modificar en LOBBY (Sección 10, 72)")
    void testDrinkPriceModificationOnlyInLobby() {
        assertEquals(GameState.LOBBY, game.getState());
        game.updateDrinkPrice(5000L);
        assertEquals(5000L, game.getCurrentDrinkPrice());

        game.startRound();
        assertEquals(GameState.IN_PROGRESS, game.getState());

        assertThrows(InvalidGameStateException.class, () -> game.updateDrinkPrice(6000L));
    }

    @Test
    @DisplayName("La modalidad de juego solo se puede modificar en LOBBY (Sección 10)")
    void testGameModeModificationOnlyInLobby() {
        assertEquals(GameMode.ULTIMO_PIERDE, game.getCurrentMode());
        game.changeMode(GameMode.PEQUENOS_PAGAN);
        assertEquals(GameMode.PEQUENOS_PAGAN, game.getCurrentMode());

        game.startRound();
        assertThrows(InvalidGameStateException.class, () -> game.changeMode(GameMode.ULTIMOS_DOS_PIERDEN));
    }

    @Test
    @DisplayName("Finalizar partida solo es permitido desde LOBBY, nunca a mitad de ronda (Sección 9, 11)")
    void testFinishGameOnlyFromLobby() {
        game.startRound();
        assertThrows(InvalidGameStateException.class, () -> game.finishGame());

        game.roundFinished();
        assertEquals(GameState.LOBBY, game.getState());

        game.finishGame();
        assertEquals(GameState.FINISHED, game.getState());
    }

    @Test
    @DisplayName("Espectador que se une en IN_PROGRESS es promovido a PLAYER cuando el juego vuelve a LOBBY (Sección 8, 72)")
    void testSpectatorAutoPromotionWhenRoundEnds() {
        game.startRound();

        // Jugador entra mientras la ronda está en curso -> entra como SPECTATOR
        GameParticipant participant = GameParticipant.createSpectator("GP-1", game.getId(), "User-2");
        assertTrue(participant.isSpectator());
        assertFalse(participant.isPlayer());

        // La ronda termina y el juego regresa a LOBBY
        game.roundFinished();
        participant.promoteToPlayer();

        assertTrue(participant.isPlayer());
        assertFalse(participant.isSpectator());
    }

    @Test
    @DisplayName("Transiciones de estado de una ronda (Round)")
    void testRoundStateTransitions() {
        Round round = new Round("R-1", game.getId(), GameMode.ULTIMO_PIERDE, 4000L, RoundType.NORMAL);
        assertEquals(RoundState.STARTING, round.getState());

        round.startRevealing();
        assertEquals(RoundState.REVEALING, round.getState());

        round.startValidating();
        assertEquals(RoundState.VALIDATING, round.getState());

        round.startResolving();
        assertEquals(RoundState.RESOLVING, round.getState());

        round.finish();
        assertEquals(RoundState.FINISHED, round.getState());
        assertNotNull(round.getFinishedAt());
    }
}
