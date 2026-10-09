package com.tappazo.infrastructure.persistence.repository;

import com.tappazo.infrastructure.persistence.entity.RevealEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface JpaRevealRepository extends JpaRepository<RevealEntity, String> {
    List<RevealEntity> findByRoundId(String roundId);
    Optional<RevealEntity> findByRoundIdAndPlayerId(String roundId, String playerId);
}
