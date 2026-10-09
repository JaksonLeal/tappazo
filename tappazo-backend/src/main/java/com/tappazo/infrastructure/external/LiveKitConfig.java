package com.tappazo.infrastructure.external;

import io.livekit.server.RoomServiceClient;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * Configuración del servidor WebRTC LiveKit para audio/video en tiempo real.
 * Sección 27 del documento maestro.
 */
@Configuration
public class LiveKitConfig {

    @Value("${tappazo.livekit.api-key:devkey}")
    private String apiKey;

    @Value("${tappazo.livekit.api-secret:secret}")
    private String apiSecret;

    @Value("${tappazo.livekit.server-url:ws://localhost:7880}")
    private String serverUrl;

    @Bean
    public RoomServiceClient roomServiceClient() {
        String httpUrl = toHttpUrl(serverUrl);
        return RoomServiceClient.create(httpUrl, apiKey, apiSecret);
    }

    public static String toHttpUrl(String url) {
        if (url == null || url.isBlank()) {
            return "http://localhost:7880";
        }
        if (url.startsWith("ws://")) {
            return "http://" + url.substring(5);
        } else if (url.startsWith("wss://")) {
            return "https://" + url.substring(6);
        }
        return url;
    }

    public String getApiKey() {
        return apiKey;
    }

    public String getApiSecret() {
        return apiSecret;
    }

    public String getServerUrl() {
        return serverUrl;
    }
}
