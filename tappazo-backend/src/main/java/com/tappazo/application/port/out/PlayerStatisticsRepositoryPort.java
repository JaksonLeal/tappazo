package com.tappazo.application.port.out;

import com.tappazo.domain.model.PlayerStatistics;

import java.util.Optional;

public interface PlayerStatisticsRepositoryPort {
    PlayerStatistics save(PlayerStatistics statistics);
    Optional<PlayerStatistics> findByUserId(String userId);
}
