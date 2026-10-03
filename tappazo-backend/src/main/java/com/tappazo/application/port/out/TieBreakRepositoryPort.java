package com.tappazo.application.port.out;

import com.tappazo.domain.model.TieBreak;

import java.util.Optional;

public interface TieBreakRepositoryPort {
    TieBreak save(TieBreak tieBreak);
    Optional<TieBreak> findById(String id);
    Optional<TieBreak> findBySourceRoundId(String sourceRoundId);
}
