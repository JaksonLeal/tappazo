package com.tappazo.infrastructure.persistence.repository;

import com.tappazo.infrastructure.persistence.entity.PlayerStatisticsEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface JpaPlayerStatisticsRepository extends JpaRepository<PlayerStatisticsEntity, String> {
    Optional<PlayerStatisticsEntity> findByUserId(String userId);
}
