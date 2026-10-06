package com.tappazo.infrastructure.websocket;

import com.tappazo.application.port.out.GameNotificationPort;
import com.tappazo.domain.model.*;
import com.tappazo.infrastructure.web.mapper.WebMappers;
import com.tappazo.infrastructure.websocket.dto.GameWebSocketEvent;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Map;
import java.util.Objects;

@Component
public class WebSocketGameNotificationAdapter implements GameNotificationPort {

    private static final Logger log = LoggerFactory.getLogger(WebSocketGameNotificationAdapter.class);
    private static final String TOPIC_PREFIX = "/topic/game/";

    private final SimpMessagingTemplate messagingTemplate;

    public WebSocketGameNotificationAdapter(SimpMessagingTemplate messagingTemplate) {
        this.messagingTemplate = Objects.requireNonNull(messagingTemplate);
    }

    @Override
    public void notifyPlayerJoined(String gameId, GameParticipant participant) {
        String destination = TOPIC_PREFIX + gameId;
        var event = GameWebSocketEvent.of("PLAYER_JOINED", gameId, WebMappers.toParticipantResponse(participant));
        log.info("Enviando evento WebSocket {} a {}", event.type(), destination);
        messagingTemplate.convertAndSend(destination, event);
    }

    @Override
    public void notifyRoundStarted(String gameId, Round round) {
        String destination = TOPIC_PREFIX + gameId;
        var event = GameWebSocketEvent.of("ROUND_STARTED", gameId, WebMappers.toRoundResponse(round));
        log.info("Enviando evento WebSocket {} a {}", event.type(), destination);
        messagingTemplate.convertAndSend(destination, event);
    }

    @Override
    public void notifyAttemptSubmitted(String gameId, String roundId, RevealAttempt attempt) {
        String destination = TOPIC_PREFIX + gameId;
        Map<String, Object> payload = Map.of(
                "roundId", roundId,
                "attemptId", attempt.getId(),
                "revealId", attempt.getRevealId(),
                "attemptNumber", attempt.getAttemptNumber(),
                "proposedNumber", attempt.getProposedNumber(),
                "photoReference", attempt.getPhotoReference() != null ? attempt.getPhotoReference() : "",
                "status", attempt.getStatus().name()
        );
        var event = GameWebSocketEvent.of("ATTEMPT_SUBMITTED", gameId, payload);
        log.info("Enviando evento WebSocket {} a {}", event.type(), destination);
        messagingTemplate.convertAndSend(destination, event);
    }

    @Override
    public void notifyRoundResolved(String gameId, Round round, GameResult result, List<DebtMovement> movements) {
        String destination = TOPIC_PREFIX + gameId;
        var movementResponses = movements.stream().map(WebMappers::toDebtMovementResponse).toList();
        Map<String, Object> payload = Map.of(
                "roundId", round.getId(),
                "confirmedLosers", result.getConfirmedLosers(),
                "winners", result.getWinners(),
                "hasTie", result.hasTie(),
                "debtMovements", movementResponses
        );
        var event = GameWebSocketEvent.of("ROUND_RESOLVED", gameId, payload);
        log.info("Enviando evento WebSocket {} a {}", event.type(), destination);
        messagingTemplate.convertAndSend(destination, event);
    }

    @Override
    public void notifyTieBreakStarted(String gameId, TieBreak tieBreak) {
        String destination = TOPIC_PREFIX + gameId;
        Map<String, Object> payload = Map.of(
                "tieBreakId", tieBreak.getId(),
                "sourceRoundId", tieBreak.getSourceRoundId(),
                "tieType", tieBreak.getTieType(),
                "affectedLoserSlots", tieBreak.getAffectedLoserSlots(),
                "status", tieBreak.getStatus().name(),
                "allTiedCase", tieBreak.isAllTiedCase()
        );
        var event = GameWebSocketEvent.of("TIE_BREAK_STARTED", gameId, payload);
        log.info("Enviando evento WebSocket {} a {}", event.type(), destination);
        messagingTemplate.convertAndSend(destination, event);
    }

    @Override
    public void notifyTieBreakResolved(String gameId, TieBreak tieBreak) {
        String destination = TOPIC_PREFIX + gameId;
        Map<String, Object> payload = Map.of(
                "tieBreakId", tieBreak.getId(),
                "status", tieBreak.getStatus().name(),
                "resolutionMode", tieBreak.getResolutionMode() != null ? tieBreak.getResolutionMode().name() : "",
                "selectedDeciderId", tieBreak.getSelectedDeciderId() != null ? tieBreak.getSelectedDeciderId() : "",
                "tieBreakRoundId", tieBreak.getTieBreakRoundId() != null ? tieBreak.getTieBreakRoundId() : ""
        );
        var event = GameWebSocketEvent.of("TIE_BREAK_RESOLVED", gameId, payload);
        log.info("Enviando evento WebSocket {} a {}", event.type(), destination);
        messagingTemplate.convertAndSend(destination, event);
    }

    @Override
    public void notifyGameFinished(String gameId) {
        String destination = TOPIC_PREFIX + gameId;
        var event = GameWebSocketEvent.of("GAME_FINISHED", gameId, Map.of("gameId", gameId));
        log.info("Enviando evento WebSocket {} a {}", event.type(), destination);
        messagingTemplate.convertAndSend(destination, event);
    }
}
