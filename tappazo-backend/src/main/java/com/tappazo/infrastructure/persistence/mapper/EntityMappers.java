package com.tappazo.infrastructure.persistence.mapper;

import com.tappazo.domain.model.*;
import com.tappazo.infrastructure.persistence.entity.*;

import java.util.ArrayList;
import java.util.List;

public final class EntityMappers {

    private EntityMappers() {}

    public static User toDomain(UserEntity entity) {
        if (entity == null) return null;
        return new User(
                entity.getId(),
                entity.getGoogleId(),
                entity.getNickname(),
                entity.getEmail(),
                entity.getProfileImage(),
                entity.getCreatedAt(),
                entity.getUpdatedAt()
        );
    }

    public static UserEntity toEntity(User domain) {
        if (domain == null) return null;
        return new UserEntity(
                domain.getId(),
                domain.getGoogleId(),
                domain.getNickname(),
                domain.getEmail(),
                domain.getProfileImage(),
                domain.getCreatedAt(),
                domain.getUpdatedAt()
        );
    }

    public static Game toDomain(GameEntity entity) {
        if (entity == null) return null;
        return new Game(
                entity.getId(),
                entity.getCode(),
                entity.getHostId(),
                GameState.valueOf(entity.getState()),
                entity.getMaxPlayers(),
                entity.getCurrentDrinkPrice(),
                GameMode.valueOf(entity.getCurrentMode()),
                entity.getVersion(),
                entity.getCreatedAt(),
                entity.getUpdatedAt()
        );
    }

    public static GameEntity toEntity(Game domain) {
        if (domain == null) return null;
        return new GameEntity(
                domain.getId(),
                domain.getCode(),
                domain.getHostId(),
                domain.getState().name(),
                domain.getMaxPlayers(),
                domain.getCurrentDrinkPrice(),
                domain.getCurrentMode().name(),
                domain.getVersion(),
                domain.getCreatedAt(),
                domain.getUpdatedAt()
        );
    }

    public static GameParticipant toDomain(GameParticipantEntity entity) {
        if (entity == null) return null;
        return new GameParticipant(
                entity.getId(),
                entity.getGameId(),
                entity.getUserId(),
                ParticipantRole.valueOf(entity.getRole()),
                ParticipantState.valueOf(entity.getParticipantState()),
                entity.getJoinedAt()
        );
    }

    public static GameParticipantEntity toEntity(GameParticipant domain) {
        if (domain == null) return null;
        return new GameParticipantEntity(
                domain.getId(),
                domain.getGameId(),
                domain.getUserId(),
                domain.getRole().name(),
                domain.getParticipantState().name(),
                domain.getJoinedAt()
        );
    }

    public static Round toDomain(RoundEntity entity) {
        if (entity == null) return null;
        return new Round(
                entity.getId(),
                entity.getGameId(),
                GameMode.valueOf(entity.getMode()),
                RoundState.valueOf(entity.getState()),
                entity.getDrinkPrice(),
                entity.getStartedAt(),
                entity.getFinishedAt(),
                RoundType.valueOf(entity.getRoundType()),
                entity.getVersion()
        );
    }

    public static RoundEntity toEntity(Round domain) {
        if (domain == null) return null;
        return new RoundEntity(
                domain.getId(),
                domain.getGameId(),
                domain.getMode().name(),
                domain.getState().name(),
                domain.getDrinkPrice(),
                domain.getStartedAt(),
                domain.getFinishedAt(),
                domain.getRoundType().name(),
                domain.getVersion()
        );
    }

    public static RoundParticipant toDomain(RoundParticipantEntity entity) {
        if (entity == null) return null;
        return new RoundParticipant(
                entity.getId(),
                entity.getRoundId(),
                entity.getUserId(),
                entity.getNicknameSnapshot(),
                entity.getTurnOrder(),
                ParticipantState.valueOf(entity.getState()),
                entity.isConfirmedLoser(),
                entity.isWinner(),
                entity.isAutomaticLoserCandidate(),
                entity.isRemovedByDisconnection(),
                entity.isRevealInvalid()
        );
    }

    public static RoundParticipantEntity toEntity(RoundParticipant domain) {
        if (domain == null) return null;
        return new RoundParticipantEntity(
                domain.getId(),
                domain.getRoundId(),
                domain.getUserId(),
                domain.getNicknameSnapshot(),
                domain.getTurnOrder(),
                domain.getState().name(),
                domain.isConfirmedLoser(),
                domain.isWinner(),
                domain.isAutomaticLoserCandidate(),
                domain.isRemovedByDisconnection(),
                domain.isRevealInvalid()
        );
    }

    public static Vote toDomain(VoteEntity entity) {
        if (entity == null) return null;
        return new Vote(
                entity.getId(),
                entity.getRevealAttemptId(),
                entity.getVoterId(),
                Decision.valueOf(entity.getDecision()),
                entity.getCreatedAt()
        );
    }

    public static VoteEntity toEntity(Vote domain) {
        if (domain == null) return null;
        return new VoteEntity(
                domain.getId(),
                domain.getRevealAttemptId(),
                domain.getVoterId(),
                domain.getDecision().name(),
                domain.getCreatedAt()
        );
    }

    public static RevealAttempt toDomain(RevealAttemptEntity entity) {
        if (entity == null) return null;
        List<Vote> votes = entity.getVotes() != null ?
                entity.getVotes().stream().map(EntityMappers::toDomain).toList() : List.of();
        return new RevealAttempt(
                entity.getId(),
                entity.getRevealId(),
                entity.getAttemptNumber(),
                entity.getProposedNumber(),
                entity.getPhotoReference(),
                entity.getPhotoOmittedReason(),
                AttemptStatus.valueOf(entity.getStatus()),
                votes,
                entity.getCreatedAt()
        );
    }

    public static RevealAttemptEntity toEntity(RevealAttempt domain) {
        if (domain == null) return null;
        List<VoteEntity> voteEntities = domain.getVotes() != null ?
                domain.getVotes().stream().map(EntityMappers::toEntity).toList() : List.of();
        return new RevealAttemptEntity(
                domain.getId(),
                domain.getRevealId(),
                domain.getAttemptNumber(),
                domain.getProposedNumber(),
                domain.getPhotoReference(),
                domain.getPhotoOmittedReason(),
                domain.getStatus().name(),
                domain.getCreatedAt(),
                voteEntities
        );
    }

    public static Reveal toDomain(RevealEntity entity) {
        if (entity == null) return null;
        List<RevealAttempt> attempts = entity.getAttempts() != null ?
                entity.getAttempts().stream().map(EntityMappers::toDomain).toList() : List.of();
        return new Reveal(
                entity.getId(),
                entity.getRoundId(),
                entity.getPlayerId(),
                RevealStatus.valueOf(entity.getStatus()),
                entity.getOfficialNumber(),
                entity.getApprovedAt(),
                attempts
        );
    }

    public static RevealEntity toEntity(Reveal domain) {
        if (domain == null) return null;
        List<RevealAttemptEntity> attemptEntities = domain.getAttempts() != null ?
                domain.getAttempts().stream().map(EntityMappers::toEntity).toList() : List.of();
        return new RevealEntity(
                domain.getId(),
                domain.getRoundId(),
                domain.getPlayerId(),
                domain.getStatus().name(),
                domain.getOfficialNumber(),
                domain.getApprovedAt(),
                0L,
                attemptEntities
        );
    }

    public static TieBreakParticipant toDomain(TieBreakParticipantEntity entity) {
        if (entity == null) return null;
        TieBreakResolutionMode mode = entity.getDecision() != null ?
                TieBreakResolutionMode.valueOf(entity.getDecision()) : null;
        return new TieBreakParticipant(
                entity.getTieBreakId(),
                entity.getPlayerId(),
                mode,
                entity.getDecisionAt()
        );
    }

    public static TieBreakParticipantEntity toEntity(TieBreakParticipant domain) {
        if (domain == null) return null;
        String modeStr = domain.getDecision() != null ? domain.getDecision().name() : null;
        return new TieBreakParticipantEntity(
                java.util.UUID.randomUUID().toString(),
                domain.getTieBreakId(),
                domain.getPlayerId(),
                modeStr,
                domain.getDecisionAt()
        );
    }

    public static TieBreak toDomain(TieBreakEntity entity) {
        if (entity == null) return null;
        TieBreakResolutionMode mode = entity.getResolutionMode() != null ?
                TieBreakResolutionMode.valueOf(entity.getResolutionMode()) : null;
        List<TieBreakParticipant> participants = entity.getParticipants() != null ?
                entity.getParticipants().stream().map(EntityMappers::toDomain).toList() : List.of();
        return new TieBreak(
                entity.getId(),
                entity.getSourceRoundId(),
                entity.getTieType(),
                entity.getAffectedLoserSlots(),
                TieBreakStatus.valueOf(entity.getStatus()),
                mode,
                entity.getSelectedDeciderId(),
                entity.isAllTiedCase(),
                entity.getTieBreakRoundId(),
                entity.getCreatedAt(),
                entity.getResolvedAt(),
                entity.getVersion(),
                participants
        );
    }

    public static TieBreakEntity toEntity(TieBreak domain) {
        if (domain == null) return null;
        String modeStr = domain.getResolutionMode() != null ? domain.getResolutionMode().name() : null;
        List<TieBreakParticipantEntity> participantEntities = domain.getParticipants() != null ?
                domain.getParticipants().stream().map(EntityMappers::toEntity).toList() : List.of();
        return new TieBreakEntity(
                domain.getId(),
                domain.getSourceRoundId(),
                domain.getTieType(),
                domain.getAffectedLoserSlots(),
                domain.getStatus().name(),
                modeStr,
                domain.getSelectedDeciderId(),
                domain.isAllTiedCase(),
                domain.getTieBreakRoundId(),
                domain.getCreatedAt(),
                domain.getResolvedAt(),
                domain.getVersion(),
                participantEntities
        );
    }

    public static DebtMovement toDomain(DebtMovementEntity entity) {
        if (entity == null) return null;
        return new DebtMovement(
                entity.getId(),
                entity.getGameId(),
                entity.getRoundId(),
                entity.getFromPlayerId(),
                entity.getToPlayerId(),
                entity.getAmount(),
                DebtReason.valueOf(entity.getReason()),
                entity.getCreatedAt()
        );
    }

    public static DebtMovementEntity toEntity(DebtMovement domain) {
        if (domain == null) return null;
        return new DebtMovementEntity(
                domain.getId(),
                domain.getGameId(),
                domain.getRoundId(),
                domain.getFromPlayerId(),
                domain.getToPlayerId(),
                domain.getAmount(),
                domain.getReason().name(),
                domain.getCreatedAt()
        );
    }

    public static PlayerStatistics toDomain(PlayerStatisticsEntity entity) {
        if (entity == null) return null;
        return new PlayerStatistics(
                entity.getUserId(),
                entity.getRoundsPlayed(),
                entity.getRoundsWon(),
                entity.getRoundsLost(),
                entity.getMoneySpent(),
                entity.getMoneyReceived()
        );
    }

    public static PlayerStatisticsEntity toEntity(PlayerStatistics domain) {
        if (domain == null) return null;
        return new PlayerStatisticsEntity(
                domain.getUserId(),
                domain.getRoundsPlayed(),
                domain.getRoundsWon(),
                domain.getRoundsLost(),
                domain.getMoneySpent(),
                domain.getMoneyReceived()
        );
    }
}
