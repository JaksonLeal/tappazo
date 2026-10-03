package com.tappazo.application.port.out;

import com.tappazo.domain.model.Round;

import java.util.List;
import java.util.Optional;

public interface RoundRepositoryPort {
    Round save(Round round);
    Optional<Round> findById(String id);
    List<Round> findByGameId(String gameId);
    Optional<Round> findCurrentRoundByGameId(String gameId);
}
