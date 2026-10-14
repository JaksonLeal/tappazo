import 'package:dio/dio.dart';

/// Traduce errores técnicos (Dio, sockets, plataforma) a mensajes legibles
/// en español para mostrar en la UI. Nunca expone trazas ni URLs firmadas.
String describeError(Object error, {String fallback = 'Ocurrió un error inesperado.'}) {
  if (error is DioException) {
    switch (error.type) {
      case DioExceptionType.connectionTimeout:
      case DioExceptionType.sendTimeout:
      case DioExceptionType.receiveTimeout:
        return 'La conexión tardó demasiado. Revisa tu red e inténtalo de nuevo.';
      case DioExceptionType.connectionError:
        return 'No se pudo conectar con el servidor. Revisa tu conexión a internet.';
      case DioExceptionType.cancel:
        return 'La operación se canceló.';
      case DioExceptionType.badCertificate:
        return 'El certificado del servidor no es válido.';
      case DioExceptionType.badResponse:
        final status = error.response?.statusCode ?? 0;
        final serverMessage = _serverMessage(error.response?.data);
        if (status == 401) return 'Tu sesión expiró. Vuelve a iniciar sesión.';
        if (status == 403) return serverMessage ?? 'No tienes permiso para esta acción.';
        if (status == 404) return serverMessage ?? 'El recurso solicitado no existe.';
        if (status >= 500) return serverMessage ?? 'El servidor tuvo un problema. Inténtalo en unos segundos.';
        return serverMessage ?? 'La solicitud fue rechazada ($status).';
      default:
        // unknown y tipos futuros de Dio (p. ej. transformTimeout).
        return 'Error de red. Revisa tu conexión.';
    }
  }
  final text = error.toString();
  if (text.contains('SocketException') || text.contains('Failed host lookup')) {
    return 'Sin conexión a internet.';
  }
  return fallback;
}

/// `true` para errores transitorios que vale la pena reintentar.
bool isTransientNetworkError(Object error) {
  if (error is DioException) {
    return error.type == DioExceptionType.connectionError ||
        error.type == DioExceptionType.connectionTimeout ||
        error.type == DioExceptionType.sendTimeout ||
        error.type == DioExceptionType.receiveTimeout ||
        (error.type == DioExceptionType.badResponse && (error.response?.statusCode ?? 0) >= 500);
  }
  return false;
}

String? _serverMessage(dynamic data) {
  if (data is Map) {
    final message = data['message'] ?? data['error'] ?? data['detail'];
    if (message is String && message.isNotEmpty && message.length < 200) return message;
  }
  return null;
}
