package com.tappazo.domain.rule;

import com.tappazo.domain.model.GameMode;
import com.tappazo.domain.service.LoserSlotResolver;

import java.util.EnumMap;
import java.util.Map;
import java.util.Objects;

/**
 * Registro / Factory para consultar la implementación correspondiente de GameModeRule (Sección 28, 45).
 */
public class GameModeRuleRegistry {

    private final Map<GameMode, GameModeRule> rules;

    public GameModeRuleRegistry() {
        this(new LoserSlotResolver());
    }

    public GameModeRuleRegistry(LoserSlotResolver resolver) {
        this.rules = new EnumMap<>(GameMode.class);
        this.rules.put(GameMode.ULTIMO_PIERDE, new LastLosesRule(resolver));
        this.rules.put(GameMode.PEQUENOS_PAGAN, new SmallNumbersPayRule(resolver));
        this.rules.put(GameMode.ULTIMOS_DOS_PIERDEN, new LastTwoLoseRule(resolver));
    }

    public GameModeRule getRule(GameMode mode) {
        Objects.requireNonNull(mode, "mode must not be null");
        GameModeRule rule = rules.get(mode);
        if (rule == null) {
            throw new IllegalArgumentException("No hay regla registrada para el modo: " + mode);
        }
        return rule;
    }
}
