package com.tappazo.application.port.out;

import com.tappazo.domain.model.*;

import java.util.List;

public interface GameNotificationPort {
    void notifyPlayerJoined(String gameId, GameParticipant participant);
    void notifyRoundStarted(String gameId, Round round);
    void notifyAttemptSubmitted(String gameId, String roundId, RevealAttempt attempt);
    void notifyRoundResolved(String gameId, Round round, GameResult result, List<DebtMovement> movements);
    void notifyTieBreakStarted(String gameId, TieBreak tieBreak);
    void notifyTieBreakResolved(String gameId, TieBreak tieBreak);
    void notifyGameFinished(String gameId);
}
