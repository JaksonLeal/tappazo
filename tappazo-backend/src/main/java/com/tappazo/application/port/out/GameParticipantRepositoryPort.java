package com.tappazo.application.port.out;

import com.tappazo.domain.model.GameParticipant;

import java.util.List;
import java.util.Optional;

public interface GameParticipantRepositoryPort {
    GameParticipant save(GameParticipant participant);
    List<GameParticipant> saveAll(List<GameParticipant> participants);
    List<GameParticipant> findByGameId(String gameId);
    Optional<GameParticipant> findByGameIdAndUserId(String gameId, String userId);
    int countByGameId(String gameId);
}
