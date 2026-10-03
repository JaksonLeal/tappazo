package com.tappazo.domain.rule;

import com.tappazo.domain.exception.InsufficientPlayersException;
import com.tappazo.domain.model.GameMode;
import com.tappazo.domain.model.GameResult;
import com.tappazo.domain.model.PlayerResult;
import com.tappazo.domain.service.LoserSlotResolver;

import java.util.List;
import java.util.Objects;

/**
 * Modalidad 1: Último pierde (Sección 38, 39).
 * Mínimo: 2 jugadores. Loser slots: 1. El número más bajo pierde.
 */
public class LastLosesRule implements GameModeRule {
    public static final int MIN_PLAYERS = 2;
    public static final int LOSER_SLOTS = 1;

    private final LoserSlotResolver loserSlotResolver;

    public LastLosesRule() {
        this.loserSlotResolver = new LoserSlotResolver();
    }

    public LastLosesRule(LoserSlotResolver loserSlotResolver) {
        this.loserSlotResolver = Objects.requireNonNull(loserSlotResolver, "loserSlotResolver must not be null");
    }

    @Override
    public int minimoJugadores() {
        return MIN_PLAYERS;
    }

    @Override
    public GameMode getMode() {
        return GameMode.ULTIMO_PIERDE;
    }

    @Override
    public int calculateLoserSlots(int totalPlayers) {
        return LOSER_SLOTS;
    }

    @Override
    public GameResult resolve(List<PlayerResult> results) {
        if (results == null || results.size() < minimoJugadores()) {
            throw new InsufficientPlayersException("Este modo requiere al menos " + minimoJugadores() + " jugadores");
        }
        return loserSlotResolver.resolve(results, calculateLoserSlots(results.size()));
    }
}
