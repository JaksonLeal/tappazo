package com.tappazo.infrastructure.external;

import io.livekit.server.RoomServiceClient;
import org.springframework.stereotype.Component;

/**
 * LiveKitVoiceProvider según especificación de las Secciones 27 y 71.
 * Especializa LiveKitRoomAdapter manteniendo compatibilidad de nomenclatura.
 */
@Component("liveKitVoiceProvider")
public class LiveKitVoiceProvider extends LiveKitRoomAdapter {

    public LiveKitVoiceProvider(LiveKitConfig liveKitConfig, RoomServiceClient roomServiceClient) {
        super(liveKitConfig, roomServiceClient);
    }
}
