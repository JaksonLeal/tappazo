package com.tappazo.infrastructure.persistence.repository;

import com.tappazo.infrastructure.persistence.entity.GameParticipantEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface JpaGameParticipantRepository extends JpaRepository<GameParticipantEntity, String> {
    List<GameParticipantEntity> findByGameId(String gameId);
    Optional<GameParticipantEntity> findByGameIdAndUserId(String gameId, String userId);
    int countByGameId(String gameId);
}
