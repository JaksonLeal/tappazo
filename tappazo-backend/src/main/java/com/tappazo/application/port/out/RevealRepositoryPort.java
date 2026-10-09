package com.tappazo.application.port.out;

import com.tappazo.domain.model.Reveal;

import java.util.List;
import java.util.Optional;

public interface RevealRepositoryPort {
    Reveal save(Reveal reveal);
    Optional<Reveal> findById(String id);
    List<Reveal> findByRoundId(String roundId);
    Optional<Reveal> findByRoundIdAndPlayerId(String roundId, String playerId);
}
