package com.tappazo.infrastructure.web.mapper;

import com.tappazo.domain.model.*;
import com.tappazo.infrastructure.web.dto.WebDTOs.*;

public final class WebMappers {

    private WebMappers() {}

    public static GameResponse toGameResponse(Game game) {
        if (game == null) return null;
        return new GameResponse(
                game.getId(),
                game.getCode(),
                game.getHostId(),
                game.getState().name(),
                game.getMaxPlayers(),
                game.getCurrentDrinkPrice(),
                game.getCurrentMode().name(),
                game.getCreatedAt()
        );
    }

    public static GameParticipantResponse toParticipantResponse(GameParticipant participant) {
        if (participant == null) return null;
        return new GameParticipantResponse(
                participant.getId(),
                participant.getGameId(),
                participant.getUserId(),
                participant.getRole().name(),
                participant.getParticipantState().name(),
                participant.getJoinedAt()
        );
    }

    public static RoundResponse toRoundResponse(Round round) {
        if (round == null) return null;
        return new RoundResponse(
                round.getId(),
                round.getGameId(),
                round.getMode().name(),
                round.getState().name(),
                round.getDrinkPrice(),
                round.getRoundType().name(),
                round.getStartedAt(),
                round.getFinishedAt()
        );
    }

    public static DebtMovementResponse toDebtMovementResponse(DebtMovement movement) {
        if (movement == null) return null;
        return new DebtMovementResponse(
                movement.getId(),
                movement.getGameId(),
                movement.getRoundId(),
                movement.getFromPlayerId(),
                movement.getToPlayerId(),
                movement.getAmount(),
                movement.getReason().name(),
                movement.getCreatedAt()
        );
    }

    public static NetDebtResponse toNetDebtResponse(NetDebt netDebt) {
        if (netDebt == null) return null;
        return new NetDebtResponse(
                netDebt.getFromPlayerId(),
                netDebt.getToPlayerId(),
                netDebt.getAmount()
        );
    }

    public static PlayerStatisticsResponse toStatisticsResponse(PlayerStatistics stats) {
        if (stats == null) return null;
        return new PlayerStatisticsResponse(
                stats.getUserId(),
                stats.getRoundsPlayed(),
                stats.getRoundsWon(),
                stats.getRoundsLost(),
                stats.getMoneySpent(),
                stats.getMoneyReceived()
        );
    }
}
