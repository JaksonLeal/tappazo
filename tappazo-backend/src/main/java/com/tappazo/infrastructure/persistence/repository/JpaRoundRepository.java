package com.tappazo.infrastructure.persistence.repository;

import com.tappazo.infrastructure.persistence.entity.RoundEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface JpaRoundRepository extends JpaRepository<RoundEntity, String> {
    List<RoundEntity> findByGameId(String gameId);
    Optional<RoundEntity> findFirstByGameIdAndStateNot(String gameId, String state);
}
