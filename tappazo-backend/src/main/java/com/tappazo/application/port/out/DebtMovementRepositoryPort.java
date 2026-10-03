package com.tappazo.application.port.out;

import com.tappazo.domain.model.DebtMovement;

import java.util.List;

public interface DebtMovementRepositoryPort {
    DebtMovement save(DebtMovement movement);
    List<DebtMovement> saveAll(List<DebtMovement> movements);
    List<DebtMovement> findByGameId(String gameId);
    List<DebtMovement> findByRoundId(String roundId);
}
