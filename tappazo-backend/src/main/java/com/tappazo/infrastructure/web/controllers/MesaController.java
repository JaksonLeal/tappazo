package com.tappazo.infrastructure.web.controllers;

import com.tappazo.application.port.out.DebtMovementRepositoryPort;
import com.tappazo.application.port.out.GameParticipantRepositoryPort;
import com.tappazo.application.port.out.GameRepositoryPort;
import com.tappazo.application.port.out.PlayerStatisticsRepositoryPort;
import com.tappazo.domain.exception.DomainException;
import com.tappazo.domain.model.DebtMovement;
import com.tappazo.domain.model.Game;
import com.tappazo.domain.model.NetDebt;
import com.tappazo.domain.model.PlayerStatistics;
import com.tappazo.domain.service.DebtNettingService;
import com.tappazo.infrastructure.web.dto.WebDTOs.*;
import com.tappazo.infrastructure.web.mapper.WebMappers;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.Objects;

@RestController
@RequestMapping("/api/v1/mesas")
public class MesaController {

    private final GameRepositoryPort gameRepository;
    private final GameParticipantRepositoryPort participantRepository;
    private final DebtMovementRepositoryPort debtMovementRepository;
    private final PlayerStatisticsRepositoryPort statisticsRepository;
    private final DebtNettingService debtNettingService;

    public MesaController(
            GameRepositoryPort gameRepository,
            GameParticipantRepositoryPort participantRepository,
            DebtMovementRepositoryPort debtMovementRepository,
            PlayerStatisticsRepositoryPort statisticsRepository,
            DebtNettingService debtNettingService) {
        this.gameRepository = Objects.requireNonNull(gameRepository);
        this.participantRepository = Objects.requireNonNull(participantRepository);
        this.debtMovementRepository = Objects.requireNonNull(debtMovementRepository);
        this.statisticsRepository = Objects.requireNonNull(statisticsRepository);
        this.debtNettingService = Objects.requireNonNull(debtNettingService);
    }

    @GetMapping("/{gameId}")
    public ResponseEntity<MesaResponse> getMesa(@PathVariable String gameId) {
        Game game = gameRepository.findById(gameId)
                .orElseThrow(() -> new DomainException("Mesa/Partida no encontrada con ID: " + gameId));

        var participants = participantRepository.findByGameId(gameId).stream()
                .map(WebMappers::toParticipantResponse)
                .toList();

        return ResponseEntity.ok(new MesaResponse(
                game.getId(),
                game.getCode(),
                game.getHostId(),
                game.getState().name(),
                participants.size(),
                participants
        ));
    }

    @GetMapping("/{gameId}/balances")
    public ResponseEntity<MesaBalanceResponse> getMesaBalances(@PathVariable String gameId) {
        if (!gameRepository.findById(gameId).isPresent()) {
            throw new DomainException("Mesa/Partida no encontrada con ID: " + gameId);
        }

        List<DebtMovement> movements = debtMovementRepository.findByGameId(gameId);
        List<NetDebt> netDebts = debtNettingService.calculateAllNetDebts(gameId, movements);

        long totalActive = netDebts.stream().mapToLong(NetDebt::getAmount).sum();

        List<NetDebtResponse> dtoList = netDebts.stream()
                .map(WebMappers::toNetDebtResponse)
                .toList();

        return ResponseEntity.ok(new MesaBalanceResponse(
                gameId,
                dtoList,
                totalActive
        ));
    }

    @GetMapping("/{gameId}/debts")
    public ResponseEntity<List<DebtMovementResponse>> getMesaDebts(@PathVariable String gameId) {
        if (!gameRepository.findById(gameId).isPresent()) {
            throw new DomainException("Mesa/Partida no encontrada con ID: " + gameId);
        }

        List<DebtMovementResponse> list = debtMovementRepository.findByGameId(gameId).stream()
                .map(WebMappers::toDebtMovementResponse)
                .toList();

        return ResponseEntity.ok(list);
    }

    @GetMapping("/users/{userId}/statistics")
    public ResponseEntity<PlayerStatisticsResponse> getUserStatistics(@PathVariable String userId) {
        PlayerStatistics stats = statisticsRepository.findByUserId(userId)
                .orElseGet(() -> new PlayerStatistics(userId));

        return ResponseEntity.ok(WebMappers.toStatisticsResponse(stats));
    }
}
