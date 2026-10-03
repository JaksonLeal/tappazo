package com.tappazo.domain.rule;

import com.tappazo.domain.exception.InsufficientPlayersException;
import com.tappazo.domain.model.GameMode;
import com.tappazo.domain.model.GameResult;
import com.tappazo.domain.model.PlayerResult;
import com.tappazo.domain.service.LoserSlotResolver;

import java.util.List;
import java.util.Objects;

/**
 * Modalidad 2: Pequeños pagan (Sección 40, 41).
 * Mínimo: 3 jugadores. Loser slots: floor(N / 2).
 * Los números más bajos ("los pequeños") ocupan los puestos de perdedor y pagan.
 */
public class SmallNumbersPayRule implements GameModeRule {
    public static final int MIN_PLAYERS = 3;

    private final LoserSlotResolver loserSlotResolver;

    public SmallNumbersPayRule() {
        this.loserSlotResolver = new LoserSlotResolver();
    }

    public SmallNumbersPayRule(LoserSlotResolver loserSlotResolver) {
        this.loserSlotResolver = Objects.requireNonNull(loserSlotResolver, "loserSlotResolver must not be null");
    }

    @Override
    public int minimoJugadores() {
        return MIN_PLAYERS;
    }

    @Override
    public GameMode getMode() {
        return GameMode.PEQUENOS_PAGAN;
    }

    @Override
    public int calculateLoserSlots(int totalPlayers) {
        return totalPlayers / 2; // División entera trunca = floor(N / 2)
    }

    @Override
    public GameResult resolve(List<PlayerResult> results) {
        if (results == null || results.size() < minimoJugadores()) {
            throw new InsufficientPlayersException("Este modo requiere al menos " + minimoJugadores() + " jugadores");
        }
        return loserSlotResolver.resolve(results, calculateLoserSlots(results.size()));
    }
}
