package com.tappazo.domain.service;

import java.util.List;
import java.util.Random;

/**
 * Estrategia para seleccionar un jugador no empatado como árbitro del desempate (Sección 33).
 * El azar se ejecuta en el backend, nunca en Flutter.
 */
@FunctionalInterface
public interface DeciderSelector {
    String selectDecider(List<String> eligibleNonTiedPlayerIds);

    static DeciderSelector random() {
        Random random = new Random();
        return eligibleNonTiedPlayerIds -> {
            if (eligibleNonTiedPlayerIds == null || eligibleNonTiedPlayerIds.isEmpty()) {
                throw new IllegalArgumentException("No hay jugadores no empatados disponibles para decidir");
            }
            int index = random.nextInt(eligibleNonTiedPlayerIds.size());
            return eligibleNonTiedPlayerIds.get(index);
        };
    }
}
