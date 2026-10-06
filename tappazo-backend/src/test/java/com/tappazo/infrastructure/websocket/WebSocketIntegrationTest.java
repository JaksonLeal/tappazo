package com.tappazo.infrastructure.websocket;

import com.tappazo.application.usecase.ProcessTieBreakVoteUseCase;
import com.tappazo.application.usecase.SubmitAttemptUseCase;
import com.tappazo.domain.model.*;
import com.tappazo.infrastructure.websocket.dto.GameWebSocketEvent;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.Mockito;
import org.springframework.messaging.simp.SimpMessagingTemplate;

import java.security.Principal;
import java.time.Instant;
import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;

@DisplayName("WebSocket & Real-Time Events Unit & Integration Tests")
class WebSocketIntegrationTest {

    private SimpMessagingTemplate messagingTemplate;
    private WebSocketGameNotificationAdapter notificationAdapter;
    private SubmitAttemptUseCase submitAttemptUseCase;
    private ProcessTieBreakVoteUseCase processTieBreakVoteUseCase;
    private GameWebSocketController webSocketController;

    @BeforeEach
    void setUp() {
        messagingTemplate = Mockito.mock(SimpMessagingTemplate.class);
        notificationAdapter = new WebSocketGameNotificationAdapter(messagingTemplate);

        submitAttemptUseCase = Mockito.mock(SubmitAttemptUseCase.class);
        processTieBreakVoteUseCase = Mockito.mock(ProcessTieBreakVoteUseCase.class);
        webSocketController = new GameWebSocketController(
                submitAttemptUseCase,
                processTieBreakVoteUseCase,
                messagingTemplate
        );
    }

    @Test
    @DisplayName("notifyPlayerJoined publishes PLAYER_JOINED event to /topic/game/{gameId}")
    void notifyPlayerJoined_publishesEvent() {
        String gameId = "game-101";
        GameParticipant participant = GameParticipant.createPlayer(UUID.randomUUID().toString(), gameId, "user-101");

        notificationAdapter.notifyPlayerJoined(gameId, participant);

        ArgumentCaptor<GameWebSocketEvent> eventCaptor = ArgumentCaptor.forClass(GameWebSocketEvent.class);
        verify(messagingTemplate).convertAndSend(eq("/topic/game/" + gameId), eventCaptor.capture());

        GameWebSocketEvent event = eventCaptor.getValue();
        assertThat(event.type()).isEqualTo("PLAYER_JOINED");
        assertThat(event.gameId()).isEqualTo(gameId);
    }

    @Test
    @DisplayName("notifyRoundStarted publishes ROUND_STARTED event to /topic/game/{gameId}")
    void notifyRoundStarted_publishesEvent() {
        String gameId = "game-102";
        Round round = new Round("round-102", gameId, GameMode.ULTIMO_PIERDE, 4000L, RoundType.NORMAL);

        notificationAdapter.notifyRoundStarted(gameId, round);

        ArgumentCaptor<GameWebSocketEvent> eventCaptor = ArgumentCaptor.forClass(GameWebSocketEvent.class);
        verify(messagingTemplate).convertAndSend(eq("/topic/game/" + gameId), eventCaptor.capture());

        assertThat(eventCaptor.getValue().type()).isEqualTo("ROUND_STARTED");
    }

    @Test
    @DisplayName("notifyRoundResolved publishes ROUND_RESOLVED event with debt movements")
    void notifyRoundResolved_publishesEvent() {
        String gameId = "game-103";
        Round round = new Round("round-103", gameId, GameMode.ULTIMO_PIERDE, 4000L, RoundType.NORMAL);
        GameResult result = new GameResult(1, List.of("loser-1"), List.of("winner-1", "winner-2"), false, List.of(), 0, false, List.of());
        DebtMovement debt = new DebtMovement(UUID.randomUUID().toString(), gameId, round.getId(), "loser-1", "winner-1", 4000L, DebtReason.ROUND_LOSS, Instant.now());

        notificationAdapter.notifyRoundResolved(gameId, round, result, List.of(debt));

        ArgumentCaptor<GameWebSocketEvent> eventCaptor = ArgumentCaptor.forClass(GameWebSocketEvent.class);
        verify(messagingTemplate).convertAndSend(eq("/topic/game/" + gameId), eventCaptor.capture());

        assertThat(eventCaptor.getValue().type()).isEqualTo("ROUND_RESOLVED");
    }

    @Test
    @DisplayName("handleChat transmits chat event to /topic/game/{gameId}/chat")
    void handleChat_broadcastsMessage() {
        Principal principal = () -> "player-abc";
        GameWebSocketController.ChatPayload payload =
                new GameWebSocketController.ChatPayload("¡Salud a todos!", "TEXT");

        webSocketController.handleChat("game-104", payload, principal);

        ArgumentCaptor<GameWebSocketEvent> eventCaptor = ArgumentCaptor.forClass(GameWebSocketEvent.class);
        verify(messagingTemplate).convertAndSend(eq("/topic/game/game-104/chat"), eventCaptor.capture());

        assertThat(eventCaptor.getValue().type()).isEqualTo("CHAT_MESSAGE");
    }

    @Test
    @DisplayName("handleAttempt executes SubmitAttemptUseCase for authenticated player")
    void handleAttempt_executesUseCase() {
        Principal principal = () -> "player-xyz";
        GameWebSocketController.SubmitAttemptPayload payload =
                new GameWebSocketController.SubmitAttemptPayload("round-xyz", 35, "photo.jpg", null);

        webSocketController.handleAttempt("game-xyz", payload, principal);

        ArgumentCaptor<SubmitAttemptUseCase.SubmitAttemptCommand> cmdCaptor =
                ArgumentCaptor.forClass(SubmitAttemptUseCase.SubmitAttemptCommand.class);
        verify(submitAttemptUseCase).execute(cmdCaptor.capture());

        assertThat(cmdCaptor.getValue().playerId()).isEqualTo("player-xyz");
        assertThat(cmdCaptor.getValue().proposedNumber()).isEqualTo(35);
    }
}
