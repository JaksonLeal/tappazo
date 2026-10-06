package com.tappazo.infrastructure.websocket.dto;

import java.time.Instant;

public record GameWebSocketEvent<T>(
        String type,
        String gameId,
        Instant timestamp,
        T payload
) {
    public static <T> GameWebSocketEvent<T> of(String type, String gameId, T payload) {
        return new GameWebSocketEvent<>(type, gameId, Instant.now(), payload);
    }
}
