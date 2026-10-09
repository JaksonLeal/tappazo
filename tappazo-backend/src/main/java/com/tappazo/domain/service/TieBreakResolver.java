package com.tappazo.domain.service;

import com.tappazo.domain.model.*;

import java.util.*;

/**
 * Motor de desempate (Sección 32, 33, 34, 65).
 * Detecta empates en el límite, maneja las opciones de desempate (NUEVA_RONDA, DIVIDIR_CUENTA),
 * gestiona el caso "todos empatados" y garantiza que los puestos restantes queden exactamente ocupados.
 */
public class TieBreakResolver {

    private final DeciderSelector deciderSelector;
    private final LoserPortionCalculator portionCalculator;
    private final LoserSlotResolver loserSlotResolver;

    public TieBreakResolver() {
        this(DeciderSelector.random(), new LoserPortionCalculator(), new LoserSlotResolver());
    }

    public TieBreakResolver(DeciderSelector deciderSelector, LoserPortionCalculator portionCalculator, LoserSlotResolver loserSlotResolver) {
        this.deciderSelector = Objects.requireNonNull(deciderSelector, "deciderSelector must not be null");
        this.portionCalculator = Objects.requireNonNull(portionCalculator, "portionCalculator must not be null");
        this.loserSlotResolver = Objects.requireNonNull(loserSlotResolver, "loserSlotResolver must not be null");
    }

    /**
     * Crea una entidad TieBreak a partir de un GameResult con empate.
     */
    public TieBreak initiateTieBreak(String tieBreakId, String roundId, GameResult gameResult) {
        if (!gameResult.hasTie()) {
            throw new IllegalArgumentException("No se puede iniciar desempate si no hay empate en el resultado");
        }
        TieBreak tieBreak = new TieBreak(
                tieBreakId,
                roundId,
                "BOUNDARY_TIE",
                gameResult.getRemainingLoserSlots(),
                gameResult.isAllTied()
        );

        for (String tiedPlayerId : gameResult.getTiedPlayers()) {
            tieBreak.addParticipant(new TieBreakParticipant(tieBreakId, tiedPlayerId));
        }

        return tieBreak;
    }

    /**
     * Resuelve el modo de desempate a aplicar según las decisiones de los jugadores (Sección 33).
     *
     * @param tieBreak entidad TieBreak
     * @param votes votos emitidos por los participantes empatados
     * @param nonTiedPlayerIds IDs de jugadores no empatados de la ronda (árbitros potenciales)
     * @param isSecondConsecutiveAllTied true si es el segundo empate consecutivo donde todos empataron
     * @return TieBreakResolutionMode resuelto, o null si se seleccionó un árbitro y se espera su voto
     */
    public TieBreakResolutionMode resolveTieBreakMode(TieBreak tieBreak,
                                                     Map<String, TieBreakResolutionMode> votes,
                                                     List<String> nonTiedPlayerIds,
                                                     boolean isSecondConsecutiveAllTied) {
        // Caso especial: todos los jugadores de la ronda quedaron empatados (Sección 33)
        if (tieBreak.isAllTiedCase()) {
            if (isSecondConsecutiveAllTied) {
                // Segundo empate consecutivo total -> DIVIDIR_CUENTA automáticamente
                tieBreak.resolve(TieBreakResolutionMode.DIVIDIR_CUENTA, null, null);
                return TieBreakResolutionMode.DIVIDIR_CUENTA;
            } else {
                // Primer empate total -> NUEVA_RONDA automática
                tieBreak.resolve(TieBreakResolutionMode.NUEVA_RONDA, null, null);
                return TieBreakResolutionMode.NUEVA_RONDA;
            }
        }

        // Si no todos están empatados, revisar votos de los empatados
        if (votes == null || votes.isEmpty()) {
            throw new IllegalArgumentException("No se han provisto decisiones para el desempate");
        }

        Set<TieBreakResolutionMode> distinctChoices = new HashSet<>(votes.values());

        // Si todos coinciden en la misma opción (Sección 33)
        if (distinctChoices.size() == 1) {
            TieBreakResolutionMode agreedMode = distinctChoices.iterator().next();
            tieBreak.resolve(agreedMode, null, null);
            return agreedMode;
        }

        // Si hay desacuerdo: se elige un jugador no empatado al azar en el backend (Sección 33)
        String deciderId = deciderSelector.selectDecider(nonTiedPlayerIds);
        // Marcamos el árbitro seleccionado en el TieBreak
        tieBreak.resolve(TieBreakResolutionMode.NUEVA_RONDA, deciderId, null); // Modo provisional o esperar decisión del árbitro
        return null; // Requiere que el árbitro decida
    }

    /**
     * Aplica la decisión tomada por el árbitro no-empatado seleccionado (Sección 33).
     */
    public void applyDeciderDecision(TieBreak tieBreak, String deciderId, TieBreakResolutionMode chosenMode) {
        if (!deciderId.equals(tieBreak.getSelectedDeciderId())) {
            throw new IllegalArgumentException("El usuario " + deciderId + " no es el árbitro seleccionado");
        }
        tieBreak.resolve(chosenMode, deciderId, null);
    }

    /**
     * Resuelve los puestos de perdedor restantes tras jugarse la NUEVA_RONDA de desempate (Sección 34).
     * Solo los competidores empatados de la ronda anterior compiten por los puestos restantes.
     * Los perdedores ya confirmados y los ganadores iniciales mantienen su estado.
     *
     * @param initialResult resultado de la ronda original
     * @param tieBreakRoundResults resultados de la nueva ronda (destaparon todos)
     * @return GameResult final definitivo con el número exacto de perdedores
     */
    public GameResult resolveRemainingSlotsFromTieBreakRound(GameResult initialResult,
                                                             List<PlayerResult> tieBreakRoundResults) {
        Set<String> tiedCompetitors = new HashSet<>(initialResult.getTiedPlayers());
        int remainingSlots = initialResult.getRemainingLoserSlots();

        // Filtrar únicamente los competidores del desempate (Sección 34: TieBreakCompetitors != RoundParticipants)
        List<PlayerResult> filteredCompetitors = new ArrayList<>();
        for (PlayerResult pr : tieBreakRoundResults) {
            if (tiedCompetitors.contains(pr.getPlayerId())) {
                filteredCompetitors.add(pr);
            }
        }

        // Resolver los puestos restantes entre los competidores
        GameResult tieBreakResult = loserSlotResolver.resolve(filteredCompetitors, remainingSlots);

        // Combinar con los perdedores y ganadores ya confirmados de la ronda inicial
        List<String> finalConfirmedLosers = new ArrayList<>(initialResult.getConfirmedLosers());
        finalConfirmedLosers.addAll(tieBreakResult.getConfirmedLosers());

        List<String> finalWinners = new ArrayList<>(initialResult.getWinners());
        finalWinners.addAll(tieBreakResult.getWinners());

        // Si la nueva ronda vuelve a generar empate entre los competidores
        boolean stillHasTie = tieBreakResult.hasTie();

        return new GameResult(
                initialResult.getTotalLoserSlots(),
                finalConfirmedLosers,
                finalWinners,
                stillHasTie,
                tieBreakResult.getTiedPlayers(),
                tieBreakResult.getRemainingLoserSlots(),
                tieBreakResult.isAllTied(),
                initialResult.getUnassignedAutomaticCandidates()
        );
    }

    public LoserPortionCalculator getPortionCalculator() {
        return portionCalculator;
    }
}
