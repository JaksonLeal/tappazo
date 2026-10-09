package com.tappazo.application.port.out;

import com.tappazo.domain.model.Game;

import java.util.Optional;

public interface GameRepositoryPort {
    Game save(Game game);
    Optional<Game> findById(String id);
    Optional<Game> findByCode(String code);
    boolean existsByCode(String code);
}
