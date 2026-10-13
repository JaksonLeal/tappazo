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

  // Configuración de timeouts
  static const Duration connectTimeout = Duration(seconds: 15);
  static const Duration receiveTimeout = Duration(seconds: 15);
}

