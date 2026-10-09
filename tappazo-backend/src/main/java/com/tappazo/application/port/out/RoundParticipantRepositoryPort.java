package com.tappazo.application.port.out;

import com.tappazo.domain.model.RoundParticipant;

import java.util.List;
import java.util.Optional;

public interface RoundParticipantRepositoryPort {
    RoundParticipant save(RoundParticipant participant);
    List<RoundParticipant> saveAll(List<RoundParticipant> participants);
    List<RoundParticipant> findByRoundId(String roundId);
    Optional<RoundParticipant> findByRoundIdAndUserId(String roundId, String userId);
}
