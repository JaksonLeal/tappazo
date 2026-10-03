package com.tappazo.domain.rule;

import com.tappazo.domain.exception.InsufficientPlayersException;
import com.tappazo.domain.model.GameMode;
import com.tappazo.domain.model.GameResult;
import com.tappazo.domain.model.PlayerResult;
import com.tappazo.domain.service.LoserSlotResolver;

import java.util.List;
import java.util.Objects;

/**
 * Modalidad 3: Últimos dos pierden (Sección 42).
 * Mínimo: 4 jugadores (ajustado para garantizar W >= L). Loser slots: 2 fijos.
 * Los dos números más bajos pierden.
 */
public class LastTwoLoseRule implements GameModeRule {
    public static final int MIN_PLAYERS = 4;
    public static final int LOSER_SLOTS = 2;

    private final LoserSlotResolver loserSlotResolver;

    public LastTwoLoseRule() {
        this.loserSlotResolver = new LoserSlotResolver();
    }

    public LastTwoLoseRule(LoserSlotResolver loserSlotResolver) {
        this.loserSlotResolver = Objects.requireNonNull(loserSlotResolver, "loserSlotResolver must not be null");
    }

    @Override
    public int minimoJugadores() {
        return MIN_PLAYERS;
    }

    @Override
    public GameMode getMode() {
        return GameMode.ULTIMOS_DOS_PIERDEN;
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
