package com.tappazo.application.fakes;

import com.tappazo.application.port.out.*;
import com.tappazo.domain.model.*;

import java.util.*;

public class InMemoryRepositories {

    public static class GameRepo implements GameRepositoryPort {
        private final Map<String, Game> games = new HashMap<>();

        @Override
        public Game save(Game game) {
            games.put(game.getId(), game);
            return game;
        }

        @Override
        public Optional<Game> findById(String id) {
            return Optional.ofNullable(games.get(id));
        }

        @Override
        public Optional<Game> findByCode(String code) {
            return games.values().stream().filter(g -> g.getCode().equalsIgnoreCase(code)).findFirst();
        }

        @Override
        public boolean existsByCode(String code) {
            return findByCode(code).isPresent();
        }
    }

    public static class GameParticipantRepo implements GameParticipantRepositoryPort {
        private final Map<String, GameParticipant> participants = new HashMap<>();

        @Override
        public GameParticipant save(GameParticipant participant) {
            participants.put(participant.getId(), participant);
            return participant;
        }

        @Override
        public List<GameParticipant> saveAll(List<GameParticipant> list) {
            list.forEach(p -> participants.put(p.getId(), p));
            return list;
        }

        @Override
        public List<GameParticipant> findByGameId(String gameId) {
            return participants.values().stream().filter(p -> p.getGameId().equals(gameId)).toList();
        }

        @Override
        public Optional<GameParticipant> findByGameIdAndUserId(String gameId, String userId) {
            return participants.values().stream()
                    .filter(p -> p.getGameId().equals(gameId) && p.getUserId().equals(userId))
                    .findFirst();
        }

        @Override
        public int countByGameId(String gameId) {
            return (int) participants.values().stream().filter(p -> p.getGameId().equals(gameId)).count();
        }
    }

    public static class RoundRepo implements RoundRepositoryPort {
        private final Map<String, Round> rounds = new HashMap<>();

        @Override
        public Round save(Round round) {
            rounds.put(round.getId(), round);
            return round;
        }

        @Override
        public Optional<Round> findById(String id) {
            return Optional.ofNullable(rounds.get(id));
        }

        @Override
        public List<Round> findByGameId(String gameId) {
            return rounds.values().stream().filter(r -> r.getGameId().equals(gameId)).toList();
        }

        @Override
        public Optional<Round> findCurrentRoundByGameId(String gameId) {
            return rounds.values().stream()
                    .filter(r -> r.getGameId().equals(gameId) && r.getState() != RoundState.FINISHED)
                    .findFirst();
        }
    }

    public static class RoundParticipantRepo implements RoundParticipantRepositoryPort {
        private final Map<String, RoundParticipant> participants = new HashMap<>();

        @Override
        public RoundParticipant save(RoundParticipant participant) {
            participants.put(participant.getId(), participant);
            return participant;
        }

        @Override
        public List<RoundParticipant> saveAll(List<RoundParticipant> list) {
            list.forEach(p -> participants.put(p.getId(), p));
            return list;
        }

        @Override
        public List<RoundParticipant> findByRoundId(String roundId) {
            return participants.values().stream().filter(p -> p.getRoundId().equals(roundId)).toList();
        }

        @Override
        public Optional<RoundParticipant> findByRoundIdAndUserId(String roundId, String userId) {
            return participants.values().stream()
                    .filter(p -> p.getRoundId().equals(roundId) && p.getUserId().equals(userId))
                    .findFirst();
        }
    }

    public static class RevealRepo implements RevealRepositoryPort {
        private final Map<String, Reveal> reveals = new HashMap<>();

        @Override
        public Reveal save(Reveal reveal) {
            reveals.put(reveal.getId(), reveal);
            return reveal;
        }

        @Override
        public Optional<Reveal> findById(String id) {
            return Optional.ofNullable(reveals.get(id));
        }

        @Override
        public List<Reveal> findByRoundId(String roundId) {
            return reveals.values().stream().filter(r -> r.getRoundId().equals(roundId)).toList();
        }

        @Override
        public Optional<Reveal> findByRoundIdAndPlayerId(String roundId, String playerId) {
            return reveals.values().stream()
                    .filter(r -> r.getRoundId().equals(roundId) && r.getPlayerId().equals(playerId))
                    .findFirst();
        }
    }

    public static class TieBreakRepo implements TieBreakRepositoryPort {
        private final Map<String, TieBreak> tieBreaks = new HashMap<>();

        @Override
        public TieBreak save(TieBreak tieBreak) {
            tieBreaks.put(tieBreak.getId(), tieBreak);
            return tieBreak;
        }

        @Override
        public Optional<TieBreak> findById(String id) {
            return Optional.ofNullable(tieBreaks.get(id));
        }

        @Override
        public Optional<TieBreak> findBySourceRoundId(String sourceRoundId) {
            return tieBreaks.values().stream().filter(t -> t.getSourceRoundId().equals(sourceRoundId)).findFirst();
        }
    }

    public static class DebtMovementRepo implements DebtMovementRepositoryPort {
        private final List<DebtMovement> movements = new ArrayList<>();

        @Override
        public DebtMovement save(DebtMovement movement) {
            movements.add(movement);
            return movement;
        }

        @Override
        public List<DebtMovement> saveAll(List<DebtMovement> list) {
            movements.addAll(list);
            return list;
        }

        @Override
        public List<DebtMovement> findByGameId(String gameId) {
            return movements.stream().filter(m -> m.getGameId().equals(gameId)).toList();
        }

        @Override
        public List<DebtMovement> findByRoundId(String roundId) {
            return movements.stream().filter(m -> m.getRoundId().equals(roundId)).toList();
        }
    }

    public static class StatisticsRepo implements PlayerStatisticsRepositoryPort {
        private final Map<String, PlayerStatistics> stats = new HashMap<>();

        @Override
        public PlayerStatistics save(PlayerStatistics statistics) {
            stats.put(statistics.getUserId(), statistics);
            return statistics;
        }

        @Override
        public Optional<PlayerStatistics> findByUserId(String userId) {
            return Optional.ofNullable(stats.get(userId));
        }
    }

    public static class NotificationPort implements GameNotificationPort {
        public int notificationsCount = 0;

        @Override public void notifyPlayerJoined(String gameId, GameParticipant participant) { notificationsCount++; }
        @Override public void notifyRoundStarted(String gameId, Round round) { notificationsCount++; }
        @Override public void notifyAttemptSubmitted(String gameId, String roundId, RevealAttempt attempt) { notificationsCount++; }
        @Override public void notifyRoundResolved(String gameId, Round round, GameResult result, List<DebtMovement> movements) { notificationsCount++; }
        @Override public void notifyTieBreakStarted(String gameId, TieBreak tieBreak) { notificationsCount++; }
        @Override public void notifyTieBreakResolved(String gameId, TieBreak tieBreak) { notificationsCount++; }
        @Override public void notifyGameFinished(String gameId) { notificationsCount++; }
    }
}
