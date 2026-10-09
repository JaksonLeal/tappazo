package com.tappazo.infrastructure.websocket;

import com.tappazo.application.usecase.ProcessTieBreakVoteUseCase;
import com.tappazo.application.usecase.SubmitAttemptUseCase;
import com.tappazo.domain.model.TieBreakResolutionMode;
import com.tappazo.infrastructure.websocket.dto.GameWebSocketEvent;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.messaging.handler.annotation.DestinationVariable;
import org.springframework.messaging.handler.annotation.MessageMapping;
import org.springframework.messaging.handler.annotation.Payload;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Controller;

import java.security.Principal;
import java.time.Instant;
import java.util.Map;
import java.util.Objects;

@Controller
public class GameWebSocketController {

    private static final Logger log = LoggerFactory.getLogger(GameWebSocketController.class);

    private final SubmitAttemptUseCase submitAttemptUseCase;
    private final ProcessTieBreakVoteUseCase processTieBreakVoteUseCase;
    private final SimpMessagingTemplate messagingTemplate;

    public GameWebSocketController(
            SubmitAttemptUseCase submitAttemptUseCase,
            ProcessTieBreakVoteUseCase processTieBreakVoteUseCase,
            SimpMessagingTemplate messagingTemplate) {
        this.submitAttemptUseCase = Objects.requireNonNull(submitAttemptUseCase);
        this.processTieBreakVoteUseCase = Objects.requireNonNull(processTieBreakVoteUseCase);
        this.messagingTemplate = Objects.requireNonNull(messagingTemplate);
    }

    public record SubmitAttemptPayload(
            String roundId,
            int proposedNumber,
            String photoReference,
            String photoOmittedReason
    ) {}

    public record TieBreakVotePayload(
            String tieBreakId,
            TieBreakResolutionMode decision
    ) {}

    public record ChatPayload(
            String message,
            String type // e.g. "TEXT", "REACTION"
    ) {}

    @MessageMapping("/game/{gameId}/attempt")
    public void handleAttempt(
            @DestinationVariable String gameId,
            @Payload SubmitAttemptPayload payload,
            Principal principal) {
        if (principal == null) {
            log.warn("Intento de revelación rechazado: usuario no autenticado en WebSocket");
            return;
        }
        String playerId = principal.getName();
        log.info("Recibido intento de revelación vía WebSocket: gameId={}, playerId={}, number={}",
                gameId, playerId, payload.proposedNumber());

        var command = new SubmitAttemptUseCase.SubmitAttemptCommand(
                payload.roundId(),
                playerId,
                payload.proposedNumber(),
                payload.photoReference(),
                payload.photoOmittedReason()
        );
        submitAttemptUseCase.execute(command);
    }

    @MessageMapping("/game/{gameId}/tiebreak-vote")
    public void handleTieBreakVote(
            @DestinationVariable String gameId,
            @Payload TieBreakVotePayload payload,
            Principal principal) {
        if (principal == null) {
            log.warn("Voto de desempate rechazado: usuario no autenticado en WebSocket");
            return;
        }
        String playerId = principal.getName();
        log.info("Recibido voto de desempate vía WebSocket: gameId={}, playerId={}, decision={}",
                gameId, playerId, payload.decision());

        var command = new ProcessTieBreakVoteUseCase.ProcessTieBreakVoteCommand(
                payload.tieBreakId(),
                playerId,
                payload.decision()
        );
        processTieBreakVoteUseCase.execute(command);
    }

    @MessageMapping("/game/{gameId}/chat")
    public void handleChat(
            @DestinationVariable String gameId,
            @Payload ChatPayload payload,
            Principal principal) {
        String senderId = principal != null ? principal.getName() : "ANONYMOUS";
        log.info("Mensaje de chat en partida {}: remitente={}, texto={}", gameId, senderId, payload.message());

        Map<String, Object> messageData = Map.of(
                "senderId", senderId,
                "message", payload.message(),
                "type", payload.type() != null ? payload.type() : "TEXT",
                "timestamp", Instant.now().toString()
        );
        messagingTemplate.convertAndSend("/topic/game/" + gameId + "/chat",
                GameWebSocketEvent.of("CHAT_MESSAGE", gameId, messageData));
    }
}
