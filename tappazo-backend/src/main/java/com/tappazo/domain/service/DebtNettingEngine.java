package com.tappazo.domain.service;

import com.tappazo.domain.model.DebtMovement;
import com.tappazo.domain.model.NetDebt;

import java.util.*;

/**
 * Motor de neteo de deudas cruzadas entre participantes (FASE 3 - Secciones 51, 52, 64).
 * Soporta neteo bilateral directo y simplificación multilateral de transacciones para la mesa.
 */
public class DebtNettingEngine {

    private final DebtNettingService nettingService;

    public DebtNettingEngine() {
        this.nettingService = new DebtNettingService();
    }

    public DebtNettingEngine(DebtNettingService nettingService) {
        this.nettingService = Objects.requireNonNull(nettingService, "nettingService must not be null");
    }

    /**
     * Resumen del estado financiero neto de un jugador en la partida.
     */
    public record PlayerFinancialSummary(
            String playerId,
            long totalOwed,     // Lo que debe pagar en bruto
            long totalReceivable, // Lo que debe recibir en bruto
            long netBalance     // Positivo = saldo a favor, Negativo = saldo en contra, 0 = equilibrado
    ) {}

    /**
     * Calcula los saldos netos bilaterales de todos los pares en la partida (Sección 52).
     */
    public List<NetDebt> calculateBilateralNetDebts(String gameId, List<DebtMovement> movements) {
        return nettingService.calculateAllNetDebts(gameId, movements);
    }

    /**
     * Calcula la posición neta individual de cada participante en la partida.
     */
    public Map<String, PlayerFinancialSummary> calculatePlayerSummaries(String gameId, List<DebtMovement> movements) {
        Objects.requireNonNull(gameId, "gameId must not be null");
        if (movements == null) return Collections.emptyMap();

        Map<String, Long> owedMap = new HashMap<>();
        Map<String, Long> receivableMap = new HashMap<>();
        Set<String> allPlayers = new HashSet<>();

        for (DebtMovement dm : movements) {
            if (!gameId.equals(dm.getGameId())) continue;

            allPlayers.add(dm.getFromPlayerId());
            allPlayers.add(dm.getToPlayerId());

            owedMap.merge(dm.getFromPlayerId(), dm.getAmount(), Long::sum);
            receivableMap.merge(dm.getToPlayerId(), dm.getAmount(), Long::sum);
        }

        Map<String, PlayerFinancialSummary> result = new HashMap<>();
        for (String player : allPlayers) {
            long owed = owedMap.getOrDefault(player, 0L);
            long rec = receivableMap.getOrDefault(player, 0L);
            long net = rec - owed; // Positivo si gana más de lo que debe
            result.put(player, new PlayerFinancialSummary(player, owed, rec, net));
        }

        return result;
    }

    /**
     * Algoritmo de simplificación multilateral de pagos (Minimiza el número de transacciones en la mesa).
     * Empareja a los mayores deudores netos con los mayores acreedores netos hasta liquidar todos los saldos.
     */
    public List<NetDebt> simplifyDebts(String gameId, List<DebtMovement> movements) {
        Map<String, PlayerFinancialSummary> summaries = calculatePlayerSummaries(gameId, movements);

        // Separar acreedores (net > 0) y deudores (net < 0)
        PriorityQueue<Debtor> debtors = new PriorityQueue<>((a, b) -> Long.compare(b.amount, a.amount));
        PriorityQueue<Creditor> creditors = new PriorityQueue<>((a, b) -> Long.compare(b.amount, a.amount));

        for (PlayerFinancialSummary summary : summaries.values()) {
            if (summary.netBalance() < 0) {
                debtors.offer(new Debtor(summary.playerId(), -summary.netBalance()));
            } else if (summary.netBalance() > 0) {
                creditors.offer(new Creditor(summary.playerId(), summary.netBalance()));
            }
        }

        List<NetDebt> simplifiedPayments = new ArrayList<>();

        while (!debtors.isEmpty() && !creditors.isEmpty()) {
            Debtor debtor = debtors.poll();
            Creditor creditor = creditors.poll();

            long settleAmount = Math.min(debtor.amount, creditor.amount);
            if (settleAmount > 0) {
                simplifiedPayments.add(new NetDebt(debtor.id, creditor.id, settleAmount));
            }

            if (debtor.amount > settleAmount) {
                debtors.offer(new Debtor(debtor.id, debtor.amount - settleAmount));
            }
            if (creditor.amount > settleAmount) {
                creditors.offer(new Creditor(creditor.id, creditor.amount - settleAmount));
            }
        }

        return simplifiedPayments;
    }

    private static class Debtor {
        final String id;
        final long amount;
        Debtor(String id, long amount) { this.id = id; this.amount = amount; }
    }

    private static class Creditor {
        final String id;
        final long amount;
        Creditor(String id, long amount) { this.id = id; this.amount = amount; }
    }
}
