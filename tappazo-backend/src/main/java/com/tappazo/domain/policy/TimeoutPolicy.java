package com.tappazo.domain.policy;

import java.time.Duration;
import java.time.Instant;

/**
 * Política centralizada de tiempos de espera y timeouts (Sección 18).
 * No contiene lógica de transición de dominio, solo cálculo de plazos.
 */
public final class TimeoutPolicy {

    public static final Duration ACTION_TIMEOUT = Duration.ofSeconds(15);
    public static final Duration STANDBY_WAIT = Duration.ofMinutes(1);
    public static final Duration STANDBY_TIE_WAIT = Duration.ofSeconds(30);

    private TimeoutPolicy() {
        // Utility class
    }

    public static boolean isActionTimedOut(Instant startedAt, Instant now) {
        if (startedAt == null || now == null) return false;
        return Duration.between(startedAt, now).compareTo(ACTION_TIMEOUT) >= 0;
    }

    public static boolean isStandByWaitTimedOut(Instant startedAt, Instant now) {
        if (startedAt == null || now == null) return false;
        return Duration.between(startedAt, now).compareTo(STANDBY_WAIT) >= 0;
    }

    public static boolean isStandByTieWaitTimedOut(Instant startedAt, Instant now) {
        if (startedAt == null || now == null) return false;
        return Duration.between(startedAt, now).compareTo(STANDBY_TIE_WAIT) >= 0;
    }
}
