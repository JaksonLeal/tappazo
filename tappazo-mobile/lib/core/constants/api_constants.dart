import 'dart:io' show Platform;
import 'package:flutter/foundation.dart';

class ApiConstants {
  ApiConstants._();

  // Permite sobreescribir la URL base mediante --dart-define=API_BASE_URL=http://...
  static const String _envBaseUrl = String.fromEnvironment('API_BASE_URL');

  static String get baseUrl {
    if (_envBaseUrl.isNotEmpty) {
      return _envBaseUrl;
    }
    // Android emulator utiliza 10.0.2.2 para comunicarse con el host localhost
    if (!kIsWeb && Platform.isAndroid) {
      return 'http://10.0.2.2:8080/api/v1';
    }
    // iOS simulator, desktop o web
    return 'http://localhost:8080/api/v1';
  }

  // Endpoints de autenticación
  static const String authGoogle = '/auth/google';
  static const String authLocal = '/auth/local';

  // Endpoints de juegos y mesas
  static const String games = '/games';
  static const String joinGame = '/games/join';
  static const String mesas = '/mesas';
  static const String mediaUpload = '/media/presigned-upload';
  static const String mediaDownload = '/media/presigned-download';

  // ─── Voz / LiveKit (Secciones 27 y 67) ─────────────────────────────────────
  static String voiceToken(String gameId) => '$games/$gameId/voice-token';

  /// Sustituye el `serverUrl` que devuelve el backend, útil en emulador o
  /// dispositivo físico: --dart-define=LIVEKIT_URL=ws://192.168.1.10:7880
  static const String liveKitUrlOverride = String.fromEnvironment('LIVEKIT_URL');

  // ─── WebSocket / STOMP (Sección 68) ────────────────────────────────────────
  // Permite sobreescribir mediante --dart-define=WS_URL=ws://host:8080/ws
  static const String _envWsUrl = String.fromEnvironment('WS_URL');

  /// Endpoint STOMP nativo `/ws`, derivado de [baseUrl] si no se define WS_URL.
  static String get wsUrl {
    if (_envWsUrl.isNotEmpty) {
      return _envWsUrl;
    }
    final api = Uri.parse(baseUrl);
    return api
        .replace(
          scheme: api.scheme == 'https' ? 'wss' : 'ws',
          path: '/ws',
        )
        .toString();
  }

  static String gameTopic(String gameId) => '/topic/game/$gameId';
  static String attemptDestination(String gameId) => '/app/game/$gameId/attempt';
  static String tieBreakVoteDestination(String gameId) => '/app/game/$gameId/tiebreak-vote';

  /// Destino propuesto para votar SÍ/NO sobre un RevealAttempt.
  /// El backend todavía NO tiene handler para esta acción (Sección 67), por
  /// lo que el envío queda desactivado salvo --dart-define=REVEAL_VOTING_ENABLED=true.
  static String revealVoteDestination(String gameId) => '/app/game/$gameId/vote';
  static const bool revealVotingEnabled = bool.fromEnvironment('REVEAL_VOTING_ENABLED');

  // Configuración de timeouts
  static const Duration connectTimeout = Duration(seconds: 15);
  static const Duration receiveTimeout = Duration(seconds: 15);
}

