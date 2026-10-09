package com.tappazo.domain.service;

import com.tappazo.domain.model.DebtMovement;
import com.tappazo.domain.model.NetDebt;

import java.util.*;

/**
 * Servicio de neteo de deudas (Sección 51, 52, 64).
 * Calcula saldos netos entre jugadores dentro del alcance de un gameId.
 * saldo(A -> B) = deuda(A -> B) - deuda(B -> A)
 */
public class DebtNettingService {

    /**
     * Calcula el saldo neto directo entre dos jugadores específicos en una partida.
     * Retorna positivo si playerA debe a playerB, negativo si playerB debe a playerA, o 0 si cancelan.
     */
    public long getNetBalance(String gameId, String playerA, String playerB, List<DebtMovement> movements) {
        Objects.requireNonNull(gameId, "gameId must not be null");
        Objects.requireNonNull(playerA, "playerA must not be null");
        Objects.requireNonNull(playerB, "playerB must not be null");

        if (movements == null || movements.isEmpty() || playerA.equals(playerB)) {
            return 0L;
        }

        long debtAtoB = 0L;
        long debtBtoA = 0L;

        for (DebtMovement dm : movements) {
            if (!gameId.equals(dm.getGameId())) continue;

            if (playerA.equals(dm.getFromPlayerId()) && playerB.equals(dm.getToPlayerId())) {
                debtAtoB += dm.getAmount();
            } else if (playerB.equals(dm.getFromPlayerId()) && playerA.equals(dm.getToPlayerId())) {
                debtBtoA += dm.getAmount();
            }
        }

        return debtAtoB - debtBtoA;
    }

    /**
     * Calcula todas las deudas netas consolidadas entre todos los participantes de un juego.
     */
    public List<NetDebt> calculateAllNetDebts(String gameId, List<DebtMovement> movements) {
        Objects.requireNonNull(gameId, "gameId must not be null");
        if (movements == null || movements.isEmpty()) {
            return Collections.emptyList();
        }

        // Recolectar todos los participantes involucrados en este gameId
        Set<String> players = new HashSet<>();
        for (DebtMovement dm : movements) {
            if (gameId.equals(dm.getGameId())) {
                players.add(dm.getFromPlayerId());
                players.add(dm.getToPlayerId());
            }
        }

        List<String> playerList = new ArrayList<>(players);
        Collections.sort(playerList);

        List<NetDebt> result = new ArrayList<>();

        for (int i = 0; i < playerList.size(); i++) {
            for (int j = i + 1; j < playerList.size(); j++) {
                String p1 = playerList.get(i);
                String p2 = playerList.get(j);

                long balance = getNetBalance(gameId, p1, p2, movements);
                if (balance > 0) {
                    result.add(new NetDebt(p1, p2, balance));
                } else if (balance < 0) {
                    result.add(new NetDebt(p2, p1, -balance));
                }
            }
        }

        return result;
    }
}
