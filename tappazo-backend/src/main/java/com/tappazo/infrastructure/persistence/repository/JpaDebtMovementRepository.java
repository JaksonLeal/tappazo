package com.tappazo.infrastructure.persistence.repository;

import com.tappazo.infrastructure.persistence.entity.DebtMovementEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface JpaDebtMovementRepository extends JpaRepository<DebtMovementEntity, String> {
    List<DebtMovementEntity> findByGameId(String gameId);
    List<DebtMovementEntity> findByRoundId(String roundId);
}
