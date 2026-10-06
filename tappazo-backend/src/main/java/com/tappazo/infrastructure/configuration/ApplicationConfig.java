package com.tappazo.infrastructure.configuration;

import com.tappazo.application.port.out.*;
import com.tappazo.application.usecase.*;
import com.tappazo.domain.rule.*;
import com.tappazo.domain.service.*;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.List;

@Configuration
public class ApplicationConfig {

    @Bean
    public GameModeRuleRegistry gameModeRuleRegistry() {
        return new GameModeRuleRegistry();
    }

    @Bean
    public TieBreakResolver tieBreakResolver() {
        return new TieBreakResolver();
    }

    @Bean
    public BillDivisionEngine billDivisionEngine() {
        return new BillDivisionEngine();
    }

    @Bean
    public DebtCalculationEngine debtCalculationEngine() {
        return new DebtCalculationEngine();
    }

    @Bean
    public DebtNettingService debtNettingService() {
        return new DebtNettingService();
    }

    @Bean
    public CreateGameUseCase createGameUseCase(
            GameRepositoryPort gameRepository,
            GameParticipantRepositoryPort participantRepository) {
        return new CreateGameUseCase(gameRepository, participantRepository);
    }

    @Bean
    public JoinGameUseCase joinGameUseCase(
            GameRepositoryPort gameRepository,
            GameParticipantRepositoryPort participantRepository,
            GameNotificationPort notificationPort) {
        return new JoinGameUseCase(gameRepository, participantRepository, notificationPort);
    }

    @Bean
    public SubmitAttemptUseCase submitAttemptUseCase(
            RoundRepositoryPort roundRepository,
            RoundParticipantRepositoryPort roundParticipantRepository,
            RevealRepositoryPort revealRepository,
            GameNotificationPort notificationPort) {
        return new SubmitAttemptUseCase(roundRepository, roundParticipantRepository, revealRepository, notificationPort);
    }

    @Bean
    public ProcessTieBreakVoteUseCase processTieBreakVoteUseCase(
            TieBreakRepositoryPort tieBreakRepository,
            RoundRepositoryPort roundRepository,
            RoundParticipantRepositoryPort roundParticipantRepository,
            GameRepositoryPort gameRepository,
            GameParticipantRepositoryPort gameParticipantRepository,
            DebtMovementRepositoryPort debtMovementRepository,
            PlayerStatisticsRepositoryPort statisticsRepository,
            GameNotificationPort notificationPort,
            TieBreakResolver tieBreakResolver,
            BillDivisionEngine billDivisionEngine,
            GameModeRuleRegistry ruleRegistry) {
        return new ProcessTieBreakVoteUseCase(
                tieBreakRepository,
                roundRepository,
                roundParticipantRepository,
                gameRepository,
                gameParticipantRepository,
                debtMovementRepository,
                statisticsRepository,
                notificationPort,
                tieBreakResolver,
                billDivisionEngine,
                ruleRegistry
        );
    }

    @Bean
    public ResolveRoundUseCase resolveRoundUseCase(
            RoundRepositoryPort roundRepository,
            RoundParticipantRepositoryPort roundParticipantRepository,
            GameRepositoryPort gameRepository,
            GameParticipantRepositoryPort gameParticipantRepository,
            RevealRepositoryPort revealRepository,
            TieBreakRepositoryPort tieBreakRepository,
            DebtMovementRepositoryPort debtMovementRepository,
            PlayerStatisticsRepositoryPort statisticsRepository,
            GameNotificationPort notificationPort,
            GameModeRuleRegistry ruleRegistry,
            DebtCalculationEngine debtEngine,
            TieBreakResolver tieBreakResolver) {
        return new ResolveRoundUseCase(
                roundRepository,
                roundParticipantRepository,
                gameRepository,
                gameParticipantRepository,
                revealRepository,
                tieBreakRepository,
                debtMovementRepository,
                statisticsRepository,
                notificationPort,
                ruleRegistry,
                debtEngine,
                tieBreakResolver
        );
    }
}
