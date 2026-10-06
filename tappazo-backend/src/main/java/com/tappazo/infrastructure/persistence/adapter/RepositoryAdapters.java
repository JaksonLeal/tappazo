package com.tappazo.infrastructure.persistence.adapter;

import com.tappazo.application.port.out.*;
import com.tappazo.domain.model.*;
import com.tappazo.infrastructure.persistence.entity.*;
import com.tappazo.infrastructure.persistence.mapper.EntityMappers;
import com.tappazo.infrastructure.persistence.repository.*;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Objects;
import java.util.Optional;

public final class RepositoryAdapters {

    private RepositoryAdapters() {}

    @Component
    public static class GameRepositoryAdapter implements GameRepositoryPort {
        private final JpaGameRepository jpaRepo;

        public GameRepositoryAdapter(JpaGameRepository jpaRepo) {
            this.jpaRepo = Objects.requireNonNull(jpaRepo);
        }

        @Override
        public Game save(Game game) {
            GameEntity entity = EntityMappers.toEntity(game);
            return EntityMappers.toDomain(jpaRepo.save(entity));
        }

        @Override
        public Optional<Game> findById(String id) {
            return jpaRepo.findById(id).map(EntityMappers::toDomain);
        }

        @Override
        public Optional<Game> findByCode(String code) {
            return jpaRepo.findByCode(code).map(EntityMappers::toDomain);
        }

        @Override
        public boolean existsByCode(String code) {
            return jpaRepo.existsByCode(code);
        }
    }

    @Component
    public static class GameParticipantRepositoryAdapter implements GameParticipantRepositoryPort {
        private final JpaGameParticipantRepository jpaRepo;

        public GameParticipantRepositoryAdapter(JpaGameParticipantRepository jpaRepo) {
            this.jpaRepo = Objects.requireNonNull(jpaRepo);
        }

        @Override
        public GameParticipant save(GameParticipant participant) {
            GameParticipantEntity entity = EntityMappers.toEntity(participant);
            return EntityMappers.toDomain(jpaRepo.save(entity));
        }

        @Override
        public List<GameParticipant> saveAll(List<GameParticipant> participants) {
            List<GameParticipantEntity> entities = participants.stream().map(EntityMappers::toEntity).toList();
            return jpaRepo.saveAll(entities).stream().map(EntityMappers::toDomain).toList();
        }

        @Override
        public List<GameParticipant> findByGameId(String gameId) {
            return jpaRepo.findByGameId(gameId).stream().map(EntityMappers::toDomain).toList();
        }

        @Override
        public Optional<GameParticipant> findByGameIdAndUserId(String gameId, String userId) {
            return jpaRepo.findByGameIdAndUserId(gameId, userId).map(EntityMappers::toDomain);
        }

        @Override
        public int countByGameId(String gameId) {
            return jpaRepo.countByGameId(gameId);
        }
    }

    @Component
    public static class RoundRepositoryAdapter implements RoundRepositoryPort {
        private final JpaRoundRepository jpaRepo;

        public RoundRepositoryAdapter(JpaRoundRepository jpaRepo) {
            this.jpaRepo = Objects.requireNonNull(jpaRepo);
        }

        @Override
        public Round save(Round round) {
            RoundEntity entity = EntityMappers.toEntity(round);
            return EntityMappers.toDomain(jpaRepo.save(entity));
        }

        @Override
        public Optional<Round> findById(String id) {
            return jpaRepo.findById(id).map(EntityMappers::toDomain);
        }

        @Override
        public List<Round> findByGameId(String gameId) {
            return jpaRepo.findByGameId(gameId).stream().map(EntityMappers::toDomain).toList();
        }

        @Override
        public Optional<Round> findCurrentRoundByGameId(String gameId) {
            return jpaRepo.findFirstByGameIdAndStateNot(gameId, "FINISHED").map(EntityMappers::toDomain);
        }
    }

    @Component
    public static class RoundParticipantRepositoryAdapter implements RoundParticipantRepositoryPort {
        private final JpaRoundParticipantRepository jpaRepo;

        public RoundParticipantRepositoryAdapter(JpaRoundParticipantRepository jpaRepo) {
            this.jpaRepo = Objects.requireNonNull(jpaRepo);
        }

        @Override
        public RoundParticipant save(RoundParticipant participant) {
            RoundParticipantEntity entity = EntityMappers.toEntity(participant);
            return EntityMappers.toDomain(jpaRepo.save(entity));
        }

        @Override
        public List<RoundParticipant> saveAll(List<RoundParticipant> participants) {
            List<RoundParticipantEntity> entities = participants.stream().map(EntityMappers::toEntity).toList();
            return jpaRepo.saveAll(entities).stream().map(EntityMappers::toDomain).toList();
        }

        @Override
        public List<RoundParticipant> findByRoundId(String roundId) {
            return jpaRepo.findByRoundId(roundId).stream().map(EntityMappers::toDomain).toList();
        }

        @Override
        public Optional<RoundParticipant> findByRoundIdAndUserId(String roundId, String userId) {
            return jpaRepo.findByRoundIdAndUserId(roundId, userId).map(EntityMappers::toDomain);
        }
    }

    @Component
    public static class RevealRepositoryAdapter implements RevealRepositoryPort {
        private final JpaRevealRepository jpaRepo;

        public RevealRepositoryAdapter(JpaRevealRepository jpaRepo) {
            this.jpaRepo = Objects.requireNonNull(jpaRepo);
        }

        @Override
        public Reveal save(Reveal reveal) {
            RevealEntity entity = EntityMappers.toEntity(reveal);
            return EntityMappers.toDomain(jpaRepo.save(entity));
        }

        @Override
        public Optional<Reveal> findById(String id) {
            return jpaRepo.findById(id).map(EntityMappers::toDomain);
        }

        @Override
        public List<Reveal> findByRoundId(String roundId) {
            return jpaRepo.findByRoundId(roundId).stream().map(EntityMappers::toDomain).toList();
        }

        @Override
        public Optional<Reveal> findByRoundIdAndPlayerId(String roundId, String playerId) {
            return jpaRepo.findByRoundIdAndPlayerId(roundId, playerId).map(EntityMappers::toDomain);
        }
    }

    @Component
    public static class TieBreakRepositoryAdapter implements TieBreakRepositoryPort {
        private final JpaTieBreakRepository jpaRepo;

        public TieBreakRepositoryAdapter(JpaTieBreakRepository jpaRepo) {
            this.jpaRepo = Objects.requireNonNull(jpaRepo);
        }

        @Override
        public TieBreak save(TieBreak tieBreak) {
            TieBreakEntity entity = EntityMappers.toEntity(tieBreak);
            return EntityMappers.toDomain(jpaRepo.save(entity));
        }

        @Override
        public Optional<TieBreak> findById(String id) {
            return jpaRepo.findById(id).map(EntityMappers::toDomain);
        }

        @Override
        public Optional<TieBreak> findBySourceRoundId(String sourceRoundId) {
            return jpaRepo.findBySourceRoundId(sourceRoundId).map(EntityMappers::toDomain);
        }
    }

    @Component
    public static class DebtMovementRepositoryAdapter implements DebtMovementRepositoryPort {
        private final JpaDebtMovementRepository jpaRepo;

        public DebtMovementRepositoryAdapter(JpaDebtMovementRepository jpaRepo) {
            this.jpaRepo = Objects.requireNonNull(jpaRepo);
        }

        @Override
        public DebtMovement save(DebtMovement movement) {
            DebtMovementEntity entity = EntityMappers.toEntity(movement);
            return EntityMappers.toDomain(jpaRepo.save(entity));
        }

        @Override
        public List<DebtMovement> saveAll(List<DebtMovement> movements) {
            List<DebtMovementEntity> entities = movements.stream().map(EntityMappers::toEntity).toList();
            return jpaRepo.saveAll(entities).stream().map(EntityMappers::toDomain).toList();
        }

        @Override
        public List<DebtMovement> findByGameId(String gameId) {
            return jpaRepo.findByGameId(gameId).stream().map(EntityMappers::toDomain).toList();
        }

        @Override
        public List<DebtMovement> findByRoundId(String roundId) {
            return jpaRepo.findByRoundId(roundId).stream().map(EntityMappers::toDomain).toList();
        }
    }

    @Component
    public static class PlayerStatisticsRepositoryAdapter implements PlayerStatisticsRepositoryPort {
        private final JpaPlayerStatisticsRepository jpaRepo;

        public PlayerStatisticsRepositoryAdapter(JpaPlayerStatisticsRepository jpaRepo) {
            this.jpaRepo = Objects.requireNonNull(jpaRepo);
        }

        @Override
        public PlayerStatistics save(PlayerStatistics statistics) {
            PlayerStatisticsEntity entity = EntityMappers.toEntity(statistics);
            return EntityMappers.toDomain(jpaRepo.save(entity));
        }

        @Override
        public Optional<PlayerStatistics> findByUserId(String userId) {
            return jpaRepo.findByUserId(userId).map(EntityMappers::toDomain);
        }
    }

    @Component
    public static class UserRepositoryAdapter implements UserRepositoryPort {
        private final JpaUserRepository jpaRepo;

        public UserRepositoryAdapter(JpaUserRepository jpaRepo) {
            this.jpaRepo = Objects.requireNonNull(jpaRepo);
        }

        @Override
        public User save(User user) {
            UserEntity entity = EntityMappers.toEntity(user);
            return EntityMappers.toDomain(jpaRepo.save(entity));
        }

        @Override
        public Optional<User> findById(String id) {
            return jpaRepo.findById(id).map(EntityMappers::toDomain);
        }

        @Override
        public Optional<User> findByGoogleId(String googleId) {
            return jpaRepo.findByGoogleId(googleId).map(EntityMappers::toDomain);
        }

        @Override
        public Optional<User> findByEmail(String email) {
            return jpaRepo.findByEmail(email).map(EntityMappers::toDomain);
        }

        @Override
        public boolean existsByGoogleId(String googleId) {
            return jpaRepo.existsByGoogleId(googleId);
        }
    }
}
