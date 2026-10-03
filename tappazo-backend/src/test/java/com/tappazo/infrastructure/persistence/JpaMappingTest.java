package com.tappazo.infrastructure.persistence;

import com.tappazo.infrastructure.persistence.entity.*;
import com.tappazo.infrastructure.persistence.repository.JpaRepositories;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.boot.test.autoconfigure.orm.jpa.TestEntityManager;

import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.*;

/**
 * @DataJpaTest integration tests — validates JPA mapping, relations and
 * custom query methods against an in-memory H2 database with MySQL
 * compatibility mode (schema auto-created by Hibernate create-drop).
 *
 * Spring Data discovers the nested JPA repository interfaces inside
 * JpaRepositories (a class) automatically via its default component scan.
 */
@DataJpaTest
@DisplayName("JPA Mapping Integration Tests")
class JpaMappingTest {

    @Autowired TestEntityManager em;

    @Autowired JpaRepositories.JpaUserRepository userRepo;
    @Autowired JpaRepositories.JpaGameRepository gameRepo;
    @Autowired JpaRepositories.JpaGameParticipantRepository participantRepo;
    @Autowired JpaRepositories.JpaRoundRepository roundRepo;
    @Autowired JpaRepositories.JpaRoundParticipantRepository roundParticipantRepo;
    @Autowired JpaRepositories.JpaRevealRepository revealRepo;
    @Autowired JpaRepositories.JpaDebtMovementRepository debtRepo;

    // ─── Helpers ────────────────────────────────────────────────────────────

    private UserEntity saveUser(String suffix) {
        Instant now = Instant.now();
        UserEntity u = new UserEntity(
                UUID.randomUUID().toString(),
                "google-id-" + suffix,
                "Nickname " + suffix,
                suffix + "@test.com",
                null,
                now, now);
        return em.persistAndFlush(u);
    }

    private GameEntity saveGame(String hostId, String code) {
        Instant now = Instant.now();
        GameEntity g = new GameEntity(
                UUID.randomUUID().toString(),
                code,
                hostId,
                "LOBBY",
                8,
                5000L,
                "ULTIMO_PIERDE",
                0L,
                now, now);
        return em.persistAndFlush(g);
    }

    // ─── User tests ──────────────────────────────────────────────────────────

    @Test
    @DisplayName("User: save and findByGoogleId")
    void user_findByGoogleId() {
        saveUser("A");
        Optional<UserEntity> found = userRepo.findByGoogleId("google-id-A");
        assertThat(found).isPresent();
        assertThat(found.get().getEmail()).isEqualTo("A@test.com");
        assertThat(found.get().getNickname()).isEqualTo("Nickname A");
    }

    @Test
    @DisplayName("User: findByEmail returns correct user")
    void user_findByEmail() {
        saveUser("B");
        assertThat(userRepo.findByEmail("B@test.com")).isPresent();
        assertThat(userRepo.findByEmail("nobody@test.com")).isEmpty();
    }

    @Test
    @DisplayName("User: googleId unique constraint prevents duplicate")
    void user_uniqueGoogleId() {
        saveUser("C");
        Instant now = Instant.now();
        UserEntity dup = new UserEntity(UUID.randomUUID().toString(),
                "google-id-C", "Dup", "dup@test.com", null, now, now);
        assertThatThrownBy(() -> em.persistAndFlush(dup));
    }

    // ─── Game tests ──────────────────────────────────────────────────────────

    @Test
    @DisplayName("Game: save and findByCode")
    void game_findByCode() {
        UserEntity host = saveUser("host1");
        saveGame(host.getId(), "ABC123");
        Optional<GameEntity> found = gameRepo.findByCode("ABC123");
        assertThat(found).isPresent();
        assertThat(found.get().getHostId()).isEqualTo(host.getId());
        assertThat(found.get().getCurrentMode()).isEqualTo("ULTIMO_PIERDE");
    }

    @Test
    @DisplayName("Game: existsByCode returns true for existing code")
    void game_existsByCode() {
        UserEntity host = saveUser("host2");
        saveGame(host.getId(), "XYZ789");
        assertThat(gameRepo.existsByCode("XYZ789")).isTrue();
        assertThat(gameRepo.existsByCode("NOPE00")).isFalse();
    }

    @Test
    @DisplayName("Game: code unique constraint prevents duplicate")
    void game_uniqueCode() {
        UserEntity host = saveUser("host3");
        saveGame(host.getId(), "DUP001");
        Instant now = Instant.now();
        GameEntity dup = new GameEntity(UUID.randomUUID().toString(),
                "DUP001", host.getId(), "LOBBY", 8, 5000L,
                "ULTIMO_PIERDE", 0L, now, now);
        assertThatThrownBy(() -> em.persistAndFlush(dup));
    }

    // ─── GameParticipant tests ────────────────────────────────────────────────

    @Test
    @DisplayName("GameParticipant: findByGameId returns all participants")
    void participant_findByGameId() {
        UserEntity host = saveUser("ph");
        UserEntity player = saveUser("pp");
        GameEntity game = saveGame(host.getId(), "PART01");

        GameParticipantEntity p1 = new GameParticipantEntity(
                UUID.randomUUID().toString(), game.getId(), host.getId(),
                "HOST", "ACTIVE", Instant.now());
        GameParticipantEntity p2 = new GameParticipantEntity(
                UUID.randomUUID().toString(), game.getId(), player.getId(),
                "PLAYER", "ACTIVE", Instant.now());
        em.persistAndFlush(p1);
        em.persistAndFlush(p2);

        List<GameParticipantEntity> list = participantRepo.findByGameId(game.getId());
        assertThat(list).hasSize(2);
    }

    @Test
    @DisplayName("GameParticipant: findByGameIdAndUserId finds correct entry")
    void participant_findByGameIdAndUserId() {
        UserEntity host = saveUser("ph2");
        GameEntity game = saveGame(host.getId(), "PART02");
        GameParticipantEntity p = new GameParticipantEntity(
                UUID.randomUUID().toString(), game.getId(), host.getId(),
                "HOST", "ACTIVE", Instant.now());
        em.persistAndFlush(p);

        Optional<GameParticipantEntity> found =
                participantRepo.findByGameIdAndUserId(game.getId(), host.getId());
        assertThat(found).isPresent();
        assertThat(found.get().getRole()).isEqualTo("HOST");
    }

    @Test
    @DisplayName("GameParticipant: UNIQUE(game_id, user_id) constraint enforced")
    void participant_uniqueGameUser() {
        UserEntity host = saveUser("ph3");
        GameEntity game = saveGame(host.getId(), "PART03");
        GameParticipantEntity p1 = new GameParticipantEntity(
                UUID.randomUUID().toString(), game.getId(), host.getId(),
                "HOST", "ACTIVE", Instant.now());
        em.persistAndFlush(p1);
        GameParticipantEntity dup = new GameParticipantEntity(
                UUID.randomUUID().toString(), game.getId(), host.getId(),
                "PLAYER", "ACTIVE", Instant.now());
        assertThatThrownBy(() -> em.persistAndFlush(dup));
    }

    // ─── Round tests ──────────────────────────────────────────────────────────

    @Test
    @DisplayName("Round: findByGameId returns rounds for game")
    void round_findByGameId() {
        UserEntity host = saveUser("rh");
        GameEntity game = saveGame(host.getId(), "RND001");

        RoundEntity r1 = new RoundEntity(UUID.randomUUID().toString(),
                game.getId(), "ULTIMO_PIERDE", "FINISHED",
                5000L, Instant.now(), Instant.now(), "NORMAL", 0L);
        RoundEntity r2 = new RoundEntity(UUID.randomUUID().toString(),
                game.getId(), "ULTIMO_PIERDE", "RESOLVING",
                5000L, Instant.now(), null, "NORMAL", 0L);
        em.persistAndFlush(r1);
        em.persistAndFlush(r2);

        assertThat(roundRepo.findByGameId(game.getId())).hasSize(2);
    }

    @Test
    @DisplayName("Round: findFirstByGameIdAndStateNot returns non-FINISHED round")
    void round_findCurrentRound() {
        UserEntity host = saveUser("rh2");
        GameEntity game = saveGame(host.getId(), "RND002");

        RoundEntity finished = new RoundEntity(UUID.randomUUID().toString(),
                game.getId(), "ULTIMO_PIERDE", "FINISHED",
                5000L, Instant.now(), Instant.now(), "NORMAL", 0L);
        RoundEntity active = new RoundEntity(UUID.randomUUID().toString(),
                game.getId(), "ULTIMO_PIERDE", "REVEALING",
                5000L, Instant.now(), null, "NORMAL", 0L);
        em.persistAndFlush(finished);
        em.persistAndFlush(active);

        Optional<RoundEntity> current =
                roundRepo.findFirstByGameIdAndStateNot(game.getId(), "FINISHED");
        assertThat(current).isPresent();
        assertThat(current.get().getState()).isEqualTo("REVEALING");
    }

    // ─── DebtMovement tests ──────────────────────────────────────────────────

    @Test
    @DisplayName("DebtMovement: findByGameId returns all movements for game")
    void debt_findByGameId() {
        UserEntity loser  = saveUser("dl");
        UserEntity winner = saveUser("dw");
        GameEntity game   = saveGame(loser.getId(), "DEBT01");
        RoundEntity round = new RoundEntity(UUID.randomUUID().toString(),
                game.getId(), "ULTIMO_PIERDE", "FINISHED",
                5000L, Instant.now(), Instant.now(), "NORMAL", 0L);
        em.persistAndFlush(round);

        DebtMovementEntity d1 = new DebtMovementEntity(
                UUID.randomUUID().toString(), game.getId(), round.getId(),
                loser.getId(), winner.getId(), 5000L, "GAME_DEBT", Instant.now());
        DebtMovementEntity d2 = new DebtMovementEntity(
                UUID.randomUUID().toString(), game.getId(), round.getId(),
                loser.getId(), winner.getId(), 2500L, "GAME_DEBT", Instant.now());
        em.persistAndFlush(d1);
        em.persistAndFlush(d2);

        assertThat(debtRepo.findByGameId(game.getId())).hasSize(2);
        assertThat(debtRepo.findByRoundId(round.getId())).hasSize(2);
    }

    @Test
    @DisplayName("DebtMovement: amount stored as long (COP, no float)")
    void debt_amountIsLong() {
        UserEntity loser  = saveUser("dam");
        UserEntity winner = saveUser("daw");
        GameEntity game   = saveGame(loser.getId(), "DEBT02");
        RoundEntity round = new RoundEntity(UUID.randomUUID().toString(),
                game.getId(), "PEQUENOS_PAGAN", "FINISHED",
                12500L, Instant.now(), Instant.now(), "NORMAL", 0L);
        em.persistAndFlush(round);

        long expectedAmount = 12_500L;
        DebtMovementEntity dm = new DebtMovementEntity(
                UUID.randomUUID().toString(), game.getId(), round.getId(),
                loser.getId(), winner.getId(), expectedAmount, "GAME_DEBT", Instant.now());
        em.persistAndFlush(dm);
        em.clear();

        DebtMovementEntity loaded = debtRepo.findById(dm.getId()).orElseThrow();
        assertThat(loaded.getAmount()).isEqualTo(expectedAmount);
    }
}
