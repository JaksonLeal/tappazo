package com.tappazo.domain;

import com.tappazo.domain.model.DebtMovement;
import com.tappazo.domain.model.DebtReason;
import com.tappazo.domain.model.NetDebt;
import com.tappazo.domain.model.PlayerDebtPortion;
import com.tappazo.domain.service.DebtDistributionService;
import com.tappazo.domain.service.DebtNettingService;
import com.tappazo.domain.service.LoserPortionCalculator;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

@DisplayName("Pruebas del Motor Económico (Deudas, Neteo y Porcentajes)")
class DebtCalculationTest {

    private LoserPortionCalculator portionCalculator;
    private DebtDistributionService distributionService;
    private DebtNettingService nettingService;

    @BeforeEach
    void setUp() {
        portionCalculator = new LoserPortionCalculator();
        distributionService = new DebtDistributionService();
        nettingService = new DebtNettingService();
    }

    @Test
    @DisplayName("Economía 11 jugadores en PEQUENOS_PAGAN: L=5, W=6, drinkPrice=4000 -> cada perdedor paga 4800, total 24.000 = CUENTA (Sección 48, 72)")
    void testElevenPlayersEconomics() {
        int n = 11;
        int l = 5;
        int w = 6;
        long drinkPrice = 4000;

        // Principio económico maestro (Sección 36): CUENTA = (N - L) * drinkPrice
        long cuenta = portionCalculator.calculateAccount(n, l, drinkPrice);
        assertEquals(24_000, cuenta);

        // Fórmula Sección 48: additionalPerLoser = ((W - L) * drinkPrice) / L
        long additionalPerLoser = ((w - l) * drinkPrice) / l;
        assertEquals(800, additionalPerLoser);

        long totalPerLoser = drinkPrice + additionalPerLoser;
        assertEquals(4800, totalPerLoser);

        long totalPaid = totalPerLoser * l;
        assertEquals(24_000, totalPaid);
        assertEquals(cuenta, totalPaid);

        // Verificar generación de movimientos de deuda
        List<String> losers = List.of("L1", "L2", "L3", "L4", "L5");
        List<String> winners = List.of("W1", "W2", "W3", "W4", "W5", "W6");

        List<DebtMovement> movements = distributionService.distributeNormal(
                "GAME-1", "ROUND-1", losers, winners, drinkPrice, DebtReason.ROUND_LOSS
        );

        // Cada perdedor cubre una directa a los primeros 5 ganadores (5 movs de 4000)
        // Más 5 movs de 800 de cada perdedor al 6to ganador (W6)
        assertEquals(10, movements.size());

        long totalMovementsAmount = movements.stream().mapToLong(DebtMovement::getAmount).sum();
        assertEquals(24_000, totalMovementsAmount);
    }

    @Test
    @DisplayName("División entera en COP: 4000 / 3 = 1333, nunca con decimales (Sección 46, 72)")
    void testIntegerDivisionTruncation() {
        long amount = 4000;
        long divisor = 3;
        long result = amount / divisor;

        assertEquals(1333, result);
        assertNotEquals(1333.33, (double) result);
    }

    @Test
    @DisplayName("Neteo de deudas: Carlos->Nicolas=4000, Nicolas->Carlos=4000 -> saldo=0 en mismo gameId (Sección 51, 52, 72)")
    void testDebtNettingBalancesToZero() {
        String gameId = "G-1";
        List<DebtMovement> movements = List.of(
                new DebtMovement("M1", gameId, "R1", "Carlos", "Nicolas", 4000, DebtReason.ROUND_LOSS),
                new DebtMovement("M2", gameId, "R2", "Nicolas", "Carlos", 4000, DebtReason.ROUND_LOSS)
        );

        long balance = nettingService.getNetBalance(gameId, "Carlos", "Nicolas", movements);
        assertEquals(0, balance);

        List<NetDebt> netDebts = nettingService.calculateAllNetDebts(gameId, movements);
        assertTrue(netDebts.isEmpty());
    }

    @Test
    @DisplayName("Neteo de deudas con saldo pendiente a favor (Sección 52)")
    void testDebtNettingWithPositiveBalance() {
        String gameId = "G-1";
        List<DebtMovement> movements = List.of(
                new DebtMovement("M1", gameId, "R1", "Carlos", "Nicolas", 5000, DebtReason.ROUND_LOSS),
                new DebtMovement("M2", gameId, "R2", "Nicolas", "Carlos", 2000, DebtReason.ROUND_LOSS)
        );

        long balance = nettingService.getNetBalance(gameId, "Carlos", "Nicolas", movements);
        assertEquals(3000, balance); // Carlos le debe 3000 a Nicolas

        List<NetDebt> netDebts = nettingService.calculateAllNetDebts(gameId, movements);
        assertEquals(1, netDebts.size());
        NetDebt net = netDebts.get(0);
        assertEquals("Carlos", net.getFromPlayerId());
        assertEquals("Nicolas", net.getToPlayerId());
        assertEquals(3000, net.getAmount());
    }

    @Test
    @DisplayName("Dividir cuenta: TOTAL=3, [10 confirmado, 20, 20, 20 empatados] -> 10=33,33%, cada 20=22,22%, suma=100% (Sección 37, 72)")
    void testDividirCuentaPercentages() {
        int totalPlayers = 4;
        int totalLoserSlots = 3;
        long drinkPrice = 4000;

        List<String> confirmedLosers = List.of("P10");
        List<String> tiedCandidates = List.of("P20_A", "P20_B", "P20_C");
        int remainingLoserSlots = 2;

        List<PlayerDebtPortion> portions = portionCalculator.calculatePortions(
                totalPlayers, totalLoserSlots, confirmedLosers, tiedCandidates, remainingLoserSlots, drinkPrice
        );

        assertEquals(4, portions.size());

        PlayerDebtPortion p10 = portions.stream().filter(p -> p.getPlayerId().equals("P10")).findFirst().orElseThrow();
        assertEquals(1.0 / 3.0, p10.getPercentage(), 0.0001); // 33.33%
        assertTrue(p10.isConfirmedLoser());

        for (String tiedId : tiedCandidates) {
            PlayerDebtPortion pt = portions.stream().filter(p -> p.getPlayerId().equals(tiedId)).findFirst().orElseThrow();
            assertEquals(2.0 / 9.0, pt.getPercentage(), 0.0001); // 22.22%
            assertFalse(pt.isConfirmedLoser());
        }

        // Suma de porcentajes debe ser exactamente 1.0 (100%)
        double sumPercentage = portions.stream().mapToDouble(PlayerDebtPortion::getPercentage).sum();
        assertEquals(1.0, sumPercentage, 0.000001);
    }
}
