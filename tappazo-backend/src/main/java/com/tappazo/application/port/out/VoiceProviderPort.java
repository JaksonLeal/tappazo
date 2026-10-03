package com.tappazo.application.port.out;

/**
 * Puerto de salida para proveedores de voz (Sección 27).
 * Proveedor MVP: LiveKitVoiceProvider.
 */
public interface VoiceProviderPort {

    record VoiceToken(String token, String roomName, String serverUrl) {}

    VoiceToken generateToken(String gameId, String userId);
}
