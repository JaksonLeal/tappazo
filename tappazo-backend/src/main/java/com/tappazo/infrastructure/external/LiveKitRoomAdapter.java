package com.tappazo.infrastructure.external;

import com.tappazo.application.port.out.VoiceProviderPort;
import io.livekit.server.AccessToken;
import io.livekit.server.CanPublish;
import io.livekit.server.CanSubscribe;
import io.livekit.server.RoomJoin;
import io.livekit.server.RoomName;
import io.livekit.server.RoomServiceClient;
import livekit.LivekitModels;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.annotation.Primary;
import org.springframework.stereotype.Component;

import java.time.Duration;
import java.util.List;
import java.util.Objects;

/**
 * Adaptador para interactuar con LiveKit y generar tokens de acceso a salas de voz en vivo.
 * Implementación de VoiceProviderPort según las Secciones 27 y 71 del documento maestro.
 */
@Component("liveKitRoomAdapter")
@Primary
public class LiveKitRoomAdapter implements VoiceProviderPort {

    private static final Logger log = LoggerFactory.getLogger(LiveKitRoomAdapter.class);

    private final LiveKitConfig liveKitConfig;
    private final RoomServiceClient roomServiceClient;

    public LiveKitRoomAdapter(LiveKitConfig liveKitConfig, RoomServiceClient roomServiceClient) {
        this.liveKitConfig = Objects.requireNonNull(liveKitConfig);
        this.roomServiceClient = Objects.requireNonNull(roomServiceClient);
    }

    @Override
    public VoiceToken generateToken(String gameId, String userId) {
        return generateToken(gameId, userId, userId, true, true, Duration.ofHours(2));
    }

    /**
     * Genera un token de acceso a la sala con parámetros detallados de usuario, permisos y TTL.
     */
    public VoiceToken generateToken(String gameId, String userId, String userName, boolean canPublish, boolean canSubscribe, Duration ttl) {
        String roomName = "game-" + gameId;
        AccessToken token = new AccessToken(liveKitConfig.getApiKey(), liveKitConfig.getApiSecret());
        token.setIdentity(userId);
        if (userName != null && !userName.isBlank()) {
            token.setName(userName);
        }
        if (ttl != null) {
            token.setTtl(ttl.toMillis());
        }

        token.addGrants(
                new RoomJoin(true),
                new RoomName(roomName),
                new CanPublish(canPublish),
                new CanSubscribe(canSubscribe)
        );

        String jwt = token.toJwt();
        log.info("Token de voz LiveKit generado para usuario {} en sala {}", userId, roomName);
        return new VoiceToken(jwt, roomName, liveKitConfig.getServerUrl());
    }

    /**
     * Crea explícitamente una sala en el servidor LiveKit.
     */
    public void createRoom(String roomName) {
        try {
            var response = roomServiceClient.createRoom(roomName).execute();
            if (response.isSuccessful()) {
                log.info("Sala LiveKit creada con éxito: {}", roomName);
            } else {
                log.warn("Respuesta no exitosa al crear sala LiveKit {}: {}", roomName, response.code());
            }
        } catch (Exception e) {
            log.warn("Error al crear sala LiveKit {}: {}", roomName, e.getMessage());
        }
    }

    /**
     * Elimina una sala en el servidor LiveKit.
     */
    public void deleteRoom(String roomName) {
        try {
            var response = roomServiceClient.deleteRoom(roomName).execute();
            if (response.isSuccessful()) {
                log.info("Sala LiveKit eliminada con éxito: {}", roomName);
            } else {
                log.warn("Respuesta no exitosa al eliminar sala LiveKit {}: {}", roomName, response.code());
            }
        } catch (Exception e) {
            log.warn("Error al eliminar sala LiveKit {}: {}", roomName, e.getMessage());
        }
    }

    /**
     * Obtiene la lista de participantes actuales en la sala de LiveKit.
     */
    public List<LivekitModels.ParticipantInfo> listParticipants(String roomName) {
        try {
            var response = roomServiceClient.listParticipants(roomName).execute();
            if (response.isSuccessful() && response.body() != null) {
                return response.body();
            }
        } catch (Exception e) {
            log.warn("Error al listar participantes de sala LiveKit {}: {}", roomName, e.getMessage());
        }
        return List.of();
    }
}
