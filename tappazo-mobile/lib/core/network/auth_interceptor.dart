import 'package:dio/dio.dart';
import '../storage/secure_storage_service.dart';

class AuthInterceptor extends Interceptor {
  final SecureStorageService _storageService;
  final void Function()? onUnauthorized;

  AuthInterceptor({
    required SecureStorageService storageService,
    this.onUnauthorized,
  }) : _storageService = storageService;

  @override
  Future<void> onRequest(
    RequestOptions options,
    RequestInterceptorHandler handler,
  ) async {
    // Si la ruta no es de auth público, inyectamos el Bearer token
    final path = options.path;
    final isAuthPublic = path.contains('/auth/google') || path.contains('/auth/local');

    if (!isAuthPublic) {
      final token = await _storageService.getToken();
      if (token != null && token.isNotEmpty) {
        options.headers['Authorization'] = 'Bearer $token';
      }
    }

    options.headers['Content-Type'] = 'application/json';
    options.headers['Accept'] = 'application/json';

    return handler.next(options);
  }

  @override
  void onError(DioException err, ErrorInterceptorHandler handler) {
    if (err.response?.statusCode == 401) {
      // Token expirado o inválido
      _storageService.clearAuthData();
      onUnauthorized?.call();
    }
    return handler.next(err);
  }
}

