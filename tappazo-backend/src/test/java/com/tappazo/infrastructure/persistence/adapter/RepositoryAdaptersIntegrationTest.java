package com.tappazo.infrastructure.persistence.adapter;

import com.tappazo.application.port.out.*;
import com.tappazo.domain.model.*;
import com.tappazo.infrastructure.persistence.repository.*;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;

import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

@DataJpaTest
@DisplayName("Repository Adapters (Output Ports) Integration Tests")
class RepositoryAdaptersIntegrationTest {

    @Autowired private JpaUserRepository jpaUserRepo;
    @Autowired private JpaGameRepository jpaGameRepo;
    @Autowired private JpaGameParticipantRepository jpaParticipantRepo;
    @Autowired private JpaRoundRepository jpaRoundRepo;
    @Autowired private JpaRoundParticipantRepository jpaRoundParticipantRepo;
    @Autowired private JpaRevealRepository jpaRevealRepo;
    @Autowired private JpaTieBreakRepository jpaTieBreakRepo;
    @Autowired private JpaDebtMovementRepository jpaDebtRepo;
    @Autowired private JpaPlayerStatisticsRepository jpaStatsRepo;

    private UserRepositoryPort userAdapter;
    private GameRepositoryPort gameAdapter;
    private GameParticipantRepositoryPort participantAdapter;
    private RoundRepositoryPort roundAdapter;
    private RoundParticipantRepositoryPort roundParticipantAdapter;
    private RevealRepositoryPort revealAdapter;
    private TieBreakRepositoryPort tieBreakAdapter;
    private DebtMovementRepositoryPort debtAdapter;
    private PlayerStatisticsRepositoryPort statsAdapter;

    @BeforeEach
    void setUp() {
        userAdapter = new RepositoryAdapters.UserRepositoryAdapter(jpaUserRepo);
        gameAdapter = new RepositoryAdapters.GameRepositoryAdapter(jpaGameRepo);
        participantAdapter = new RepositoryAdapters.GameParticipantRepositoryAdapter(jpaParticipantRepo);
        roundAdapter = new RepositoryAdapters.RoundRepositoryAdapter(jpaRoundRepo);
        roundParticipantAdapter = new RepositoryAdapters.RoundParticipantRepositoryAdapter(jpaRoundParticipantRepo);
        revealAdapter = new RepositoryAdapters.RevealRepositoryAdapter(jpaRevealRepo);
        tieBreakAdapter = new RepositoryAdapters.TieBreakRepositoryAdapter(jpaTieBreakRepo);
        debtAdapter = new RepositoryAdapters.DebtMovementRepositoryAdapter(jpaDebtRepo);
        statsAdapter = new RepositoryAdapters.PlayerStatisticsRepositoryAdapter(jpaStatsRepo);
    }

    @Test
    @DisplayName("UserRepositoryAdapter correctly adapts User domain model")
    void testUserAdapter() {
        String id = UUID.randomUUID().toString();
        User domainUser = new User(id, "g-123", "Nico", "nico@test.com", null, Instant.now(), Instant.now());
        User saved = userAdapter.save(domainUser);

        assertThat(saved.getId()).isEqualTo(id);
        assertThat(saved.getNickname()).isEqualTo("Nico");
        assertThat(userAdapter.findById(id)).isPresent();
        assertThat(userAdapter.findByGoogleId("g-123")).isPresent();
        assertThat(userAdapter.findByEmail("nico@test.com")).isPresent();
        assertThat(userAdapter.existsByGoogleId("g-123")).isTrue();
        assertThat(userAdapter.existsByGoogleId("g-999")).isFalse();
    }

    @Test
    @DisplayName("GameRepositoryAdapter correctly adapts Game domain model")
    void testGameAdapter() {
        String hostId = UUID.randomUUID().toString();
        userAdapter.save(new User(hostId, "g-h1", "Host", "h@t.com", null, Instant.now(), Instant.now()));

        Game domainGame = new Game(UUID.randomUUID().toString(), "CODE1", hostId, GameState.LOBBY, 6, 4000L, GameMode.ULTIMO_PIERDE, 0L, Instant.now(), Instant.now());
        Game saved = gameAdapter.save(domainGame);

        assertThat(saved.getCode()).isEqualTo("CODE1");
        assertThat(gameAdapter.findById(saved.getId())).isPresent();
        assertThat(gameAdapter.findByCode("CODE1")).isPresent();
        assertThat(gameAdapter.existsByCode("CODE1")).isTrue();
        assertThat(gameAdapter.existsByCode("NON_EXIST")).isFalse();
    }

    @Test
    @DisplayName("GameParticipantRepositoryAdapter correctly adapts GameParticipant")
    void testGameParticipantAdapter() {
        String hostId = UUID.randomUUID().toString();
        userAdapter.save(new User(hostId, "g-h2", "Host2", "h2@t.com", null, Instant.now(), Instant.now()));
        Game game = gameAdapter.save(new Game(UUID.randomUUID().toString(), "CODE2", hostId, GameState.LOBBY, 6, 4000L, GameMode.ULTIMO_PIERDE, 0L, Instant.now(), Instant.now()));

        GameParticipant p = new GameParticipant(UUID.randomUUID().toString(), game.getId(), hostId, ParticipantRole.PLAYER, ParticipantState.ACTIVE, Instant.now());
        participantAdapter.save(p);

        assertThat(participantAdapter.findByGameId(game.getId())).hasSize(1);
        assertThat(participantAdapter.findByGameIdAndUserId(game.getId(), hostId)).isPresent();
        assertThat(participantAdapter.countByGameId(game.getId())).isEqualTo(1);
    }

    @Test
    @DisplayName("RoundRepositoryAdapter and RoundParticipantAdapter correctly adapt domain models")
    void testRoundAndParticipantAdapters() {
        String hostId = UUID.randomUUID().toString();
        userAdapter.save(new User(hostId, "g-h3", "Host3", "h3@t.com", null, Instant.now(), Instant.now()));
        Game game = gameAdapter.save(new Game(UUID.randomUUID().toString(), "CODE3", hostId, GameState.IN_PROGRESS, 6, 4000L, GameMode.ULTIMO_PIERDE, 0L, Instant.now(), Instant.now()));

        Round round = new Round(UUID.randomUUID().toString(), game.getId(), GameMode.ULTIMO_PIERDE, RoundState.REVEALING, 4000L, Instant.now(), null, RoundType.NORMAL, 0L);
        roundAdapter.save(round);

        Optional<Round> current = roundAdapter.findCurrentRoundByGameId(game.getId());
        assertThat(current).isPresent();
        assertThat(current.get().getId()).isEqualTo(round.getId());

        RoundParticipant rp = new RoundParticipant(UUID.randomUUID().toString(), round.getId(), hostId, "Host3", 1, ParticipantState.ACTIVE, false, false, false, false, false);
        roundParticipantAdapter.save(rp);

        List<RoundParticipant> list = roundParticipantAdapter.findByRoundId(round.getId());
        assertThat(list).hasSize(1);
        assertThat(roundParticipantAdapter.findByRoundIdAndUserId(round.getId(), hostId)).isPresent();
    }

    @Test
    @DisplayName("RevealRepositoryAdapter correctly adapts domain model")
    void testRevealAdapter() {
        String userId = UUID.randomUUID().toString();
        userAdapter.save(new User(userId, "g-rev", "Rev", "rev@t.com", null, Instant.now(), Instant.now()));
        Game game = gameAdapter.save(new Game(UUID.randomUUID().toString(), "CODE4", userId, GameState.IN_PROGRESS, 6, 4000L, GameMode.ULTIMO_PIERDE, 0L, Instant.now(), Instant.now()));
        Round round = roundAdapter.save(new Round(UUID.randomUUID().toString(), game.getId(), GameMode.ULTIMO_PIERDE, RoundState.REVEALING, 4000L, Instant.now(), null, RoundType.NORMAL, 0L));

        Reveal reveal = new Reveal(UUID.randomUUID().toString(), round.getId(), userId, RevealStatus.APPROVED, 77, Instant.now(), List.of());
        revealAdapter.save(reveal);

        Optional<Reveal> found = revealAdapter.findByRoundIdAndPlayerId(round.getId(), userId);
        assertThat(found).isPresent();
        assertThat(found.get().getOfficialNumber()).isEqualTo(77);
    }

    @Test
    @DisplayName("TieBreakRepositoryAdapter correctly adapts domain model")
    void testTieBreakAdapter() {
        String hostId = UUID.randomUUID().toString();
        userAdapter.save(new User(hostId, "g-tb", "TBHost", "tb@t.com", null, Instant.now(), Instant.now()));
        Game game = gameAdapter.save(new Game(UUID.randomUUID().toString(), "CODE5", hostId, GameState.IN_PROGRESS, 6, 4000L, GameMode.ULTIMO_PIERDE, 0L, Instant.now(), Instant.now()));
        Round round = roundAdapter.save(new Round(UUID.randomUUID().toString(), game.getId(), GameMode.ULTIMO_PIERDE, RoundState.FINISHED, 4000L, Instant.now(), Instant.now(), RoundType.NORMAL, 0L));

        TieBreak tb = new TieBreak(UUID.randomUUID().toString(), round.getId(), "LAST_LOSES", 1, TieBreakStatus.PENDING, null, null, false, null, Instant.now(), null, 0L, List.of());
        tieBreakAdapter.save(tb);

        Optional<TieBreak> found = tieBreakAdapter.findBySourceRoundId(round.getId());
        assertThat(found).isPresent();
        assertThat(found.get().getAffectedLoserSlots()).isEqualTo(1);
    }

    @Test
    @DisplayName("DebtMovementRepositoryAdapter correctly adapts domain model")
    void testDebtMovementAdapter() {
        String p1 = UUID.randomUUID().toString();
        String p2 = UUID.randomUUID().toString();
        userAdapter.save(new User(p1, "g-p1", "P1", "p1@t.com", null, Instant.now(), Instant.now()));
        userAdapter.save(new User(p2, "g-p2", "P2", "p2@t.com", null, Instant.now(), Instant.now()));
        Game game = gameAdapter.save(new Game(UUID.randomUUID().toString(), "CODE6", p1, GameState.FINISHED, 6, 4000L, GameMode.ULTIMO_PIERDE, 0L, Instant.now(), Instant.now()));
        Round round = roundAdapter.save(new Round(UUID.randomUUID().toString(), game.getId(), GameMode.ULTIMO_PIERDE, RoundState.FINISHED, 4000L, Instant.now(), Instant.now(), RoundType.NORMAL, 0L));

        DebtMovement debt = new DebtMovement(UUID.randomUUID().toString(), game.getId(), round.getId(), p1, p2, 4000L, DebtReason.ROUND_LOSS, Instant.now());
        debtAdapter.save(debt);

        List<DebtMovement> byGame = debtAdapter.findByGameId(game.getId());
        assertThat(byGame).hasSize(1);
        assertThat(byGame.get(0).getAmount()).isEqualTo(4000L);
    }

    @Test
    @DisplayName("PlayerStatisticsRepositoryAdapter correctly adapts domain model")
    void testPlayerStatisticsAdapter() {
        String p1 = UUID.randomUUID().toString();
        userAdapter.save(new User(p1, "g-stat", "PStat", "pstat@t.com", null, Instant.now(), Instant.now()));

        PlayerStatistics stats = new PlayerStatistics(p1, 10, 7, 3, 12000L, 28000L);
        statsAdapter.save(stats);

        Optional<PlayerStatistics> found = statsAdapter.findByUserId(p1);
        assertThat(found).isPresent();
        assertThat(found.get().getRoundsPlayed()).isEqualTo(10);
        assertThat(found.get().getRoundsWon()).isEqualTo(7);
        assertThat(found.get().getRoundsLost()).isEqualTo(3);
    }
}
