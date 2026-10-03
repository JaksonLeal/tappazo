package com.tappazo.infrastructure.persistence.repository;

import com.tappazo.infrastructure.persistence.entity.RoundParticipantEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface JpaRoundParticipantRepository extends JpaRepository<RoundParticipantEntity, String> {
    List<RoundParticipantEntity> findByRoundId(String roundId);
    Optional<RoundParticipantEntity> findByRoundIdAndUserId(String roundId, String userId);
}
