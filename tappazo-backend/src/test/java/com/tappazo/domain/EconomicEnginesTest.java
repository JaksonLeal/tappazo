package com.tappazo.domain;

import com.tappazo.domain.model.DebtMovement;
import com.tappazo.domain.model.DebtReason;
import com.tappazo.domain.model.GameMode;
import com.tappazo.domain.model.NetDebt;
import com.tappazo.domain.service.BillDivisionEngine;
import com.tappazo.domain.service.DebtCalculationEngine;
import com.tappazo.domain.service.DebtNettingEngine;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

@DisplayName("Pruebas del Motor Económico Completo (Fase 3 - Secciones 36, 46-52)")
class EconomicEnginesTest {

    private DebtCalculationEngine debtEngine;
    private BillDivisionEngine billDivisionEngine;
    private DebtNettingEngine nettingEngine;

    @BeforeEach
    void setUp() {
        debtEngine = new DebtCalculationEngine();
        billDivisionEngine = new BillDivisionEngine();
        nettingEngine = new DebtNettingEngine();
    }

    @Test
    @DisplayName("DebtCalculationEngine: ULTIMO_PIERDE (5 jugadores, 1 perdedor, 4 ganadores, drinkPrice=4000)")
    void testUltimoPierdeDebtCalculation() {
        List<String> losers = List.of("L1");
        List<String> winners = List.of("W1", "W2", "W3", "W4");

        var summary = debtEngine.calculateRoundDebt("G1", "R1", GameMode.ULTIMO_PIERDE, losers, winners, 4000L);

        assertEquals(16_000L, summary.totalAccount());
        assertEquals(4000L, summary.basePerLoser());
        assertEquals(12_000L, summary.additionalPerLoser());
        assertEquals(16_000L, summary.totalPerLoser());
        assertEquals(16_000L, summary.totalPaidByLosers());
        assertEquals(0L, summary.discardedRemainder());
        assertEquals(4, summary.debtMovements().size());

        for (DebtMovement dm : summary.debtMovements()) {
            assertEquals("L1", dm.getFromPlayerId());
            assertEquals(4000L, dm.getAmount());
            assertEquals(DebtReason.ROUND_LOSS, dm.getReason());
        }
    }

    @Test
    @DisplayName("DebtCalculationEngine: PEQUENOS_PAGAN (11 jugadores, 5 perdedores, 6 ganadores, drinkPrice=4000)")
    void testPequenosPaganElevenPlayersDebtCalculation() {
        List<String> losers = List.of("L1", "L2", "L3", "L4", "L5");
        List<String> winners = List.of("W1", "W2", "W3", "W4", "W5", "W6");

        var summary = debtEngine.calculateRoundDebt("G1", "R1", GameMode.PEQUENOS_PAGAN, losers, winners, 4000L);

        assertEquals(24_000L, summary.totalAccount());
        assertEquals(4000L, summary.basePerLoser());
        assertEquals(800L, summary.additionalPerLoser()); // ((6-5)*4000)/5 = 800
        assertEquals(4800L, summary.totalPerLoser());
        assertEquals(24_000L, summary.totalPaidByLosers());
        assertEquals(0L, summary.discardedRemainder());
        assertEquals(10, summary.debtMovements().size());
    }

    @Test
    @DisplayName("DebtCalculationEngine: ULTIMOS_DOS_PIERDEN (6 jugadores, 2 perdedores, 4 ganadores, drinkPrice=4000)")
    void testUltimosDosPierdenSixPlayers() {
        List<String> losers = List.of("L1", "L2");
        List<String> winners = List.of("W1", "W2", "W3", "W4");

        var summary = debtEngine.calculateRoundDebt("G1", "R1", GameMode.ULTIMOS_DOS_PIERDEN, losers, winners, 4000L);

        assertEquals(16_000L, summary.totalAccount()); // 4 * 4000
        assertEquals(4000L, summary.basePerLoser());
        assertEquals(4000L, summary.additionalPerLoser()); // ((4-2)*4000)/2 = 4000
        assertEquals(8000L, summary.totalPerLoser());
        assertEquals(16_000L, summary.totalPaidByLosers());
        assertEquals(0L, summary.discardedRemainder());
    }

    @Test
    @DisplayName("BillDivisionEngine: DIVIDIR_CUENTA con perdedor confirmado y 3 empatados (Sección 37)")
    void testBillDivisionWithConfirmedAndTied() {
        int totalPlayers = 4;
        int totalLoserSlots = 3;
        long drinkPrice = 4000L;

        List<String> confirmed = List.of("P10");
        List<String> tied = List.of("P20_1", "P20_2", "P20_3");
        List<String> winners = List.of("Winner1");

        var summary = billDivisionEngine.calculateDividedBill(
                "G1", "R1", totalPlayers, totalLoserSlots, confirmed, tied, 2, winners, drinkPrice
        );

        assertEquals(4000L, summary.totalAccount()); // (4-3)*4000 = 4000
        assertEquals(4, summary.portions().size());

        // P10: 1/3 = 1333 COP
        var p10 = summary.portions().stream().filter(p -> p.getPlayerId().equals("P10")).findFirst().orElseThrow();
        assertEquals(1333L, p10.getAmount());

        // Cada empatado: 2/9 = 888 COP
        for (String tid : tied) {
            var pt = summary.portions().stream().filter(p -> p.getPlayerId().equals(tid)).findFirst().orElseThrow();
            assertEquals(888L, pt.getAmount());
        }

        // Total distribuido = 1333 + 3*888 = 3997. Sobrante descartado = 3 COP.
        assertEquals(3997L, summary.totalDistributedAmount());
        assertEquals(3L, summary.discardedRemainder());

        // Movimientos hacia Winner1
        assertEquals(4, summary.debtMovements().size());
        for (DebtMovement dm : summary.debtMovements()) {
            assertEquals("Winner1", dm.getToPlayerId());
            assertEquals(DebtReason.TIE_BREAK, dm.getReason());
        }
    }

    @Test
    @DisplayName("DebtNettingEngine: Neteo bilateral y resumen financiero individual (Sección 51, 52)")
    void testBilateralNettingAndSummaries() {
        String gameId = "GAME-1";
        List<DebtMovement> movements = List.of(
                new DebtMovement("1", gameId, "R1", "Alice", "Bob", 5000L, DebtReason.ROUND_LOSS),
                new DebtMovement("2", gameId, "R2", "Bob", "Alice", 2000L, DebtReason.ROUND_LOSS),
                new DebtMovement("3", gameId, "R2", "Charlie", "Alice", 4000L, DebtReason.ROUND_LOSS)
        );

        List<NetDebt> bilateral = nettingEngine.calculateBilateralNetDebts(gameId, movements);
        assertEquals(2, bilateral.size());

        // Alice le debe 3000 a Bob (5000 - 2000)
        NetDebt aliceBob = bilateral.stream()
                .filter(n -> n.getFromPlayerId().equals("Alice") && n.getToPlayerId().equals("Bob"))
                .findFirst().orElseThrow();
        assertEquals(3000L, aliceBob.getAmount());

        // Charlie le debe 4000 a Alice
        NetDebt charlieAlice = bilateral.stream()
                .filter(n -> n.getFromPlayerId().equals("Charlie") && n.getToPlayerId().equals("Alice"))
                .findFirst().orElseThrow();
        assertEquals(4000L, charlieAlice.getAmount());

        // Resumen individual
        var summaries = nettingEngine.calculatePlayerSummaries(gameId, movements);
        assertEquals(3, summaries.size());

        // Alice: debe 5000, recibe 6000 -> saldo neto +1000 a favor
        assertEquals(1000L, summaries.get("Alice").netBalance());
        // Bob: debe 2000, recibe 5000 -> saldo neto +3000 a favor
        assertEquals(3000L, summaries.get("Bob").netBalance());
        // Charlie: debe 4000, recibe 0 -> saldo neto -4000 en contra
        assertEquals(-4000L, summaries.get("Charlie").netBalance());
    }

    @Test
    @DisplayName("DebtNettingEngine: Simplificación multilateral de deudas (Sección 52, 64)")
    void testMultilateralSimplification() {
        String gameId = "GAME-1";
        // Transitive: A le debe a B 4000, B le debe a C 4000
        // En bilateral: A->B (4000), B->C (4000) = 2 transferencias
        // En simplificación: A->C (4000) = 1 sola transferencia!
        List<DebtMovement> movements = List.of(
                new DebtMovement("1", gameId, "R1", "A", "B", 4000L, DebtReason.ROUND_LOSS),
                new DebtMovement("2", gameId, "R2", "B", "C", 4000L, DebtReason.ROUND_LOSS)
        );

        List<NetDebt> simplified = nettingEngine.simplifyDebts(gameId, movements);
        assertEquals(1, simplified.size());
        assertEquals("A", simplified.get(0).getFromPlayerId());
        assertEquals("C", simplified.get(0).getToPlayerId());
        assertEquals(4000L, simplified.get(0).getAmount());
    }
}
