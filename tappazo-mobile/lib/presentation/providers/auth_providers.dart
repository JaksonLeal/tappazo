import 'package:flutter_riverpod/flutter_riverpod.dart';
import '../../core/network/api_client.dart';
import '../../core/storage/secure_storage_service.dart';
import '../../data/datasources/auth_local_datasource.dart';
import '../../data/datasources/auth_remote_datasource.dart';
import '../../data/repositories/auth_repository_impl.dart';
import '../../domain/repositories/auth_repository.dart';
import '../../domain/usecases/check_auth_status_usecase.dart';
import '../../domain/usecases/login_with_google_usecase.dart';
import '../../domain/usecases/login_with_local_usecase.dart';
import '../../domain/usecases/logout_usecase.dart';
import 'auth_notifier.dart';
import 'auth_state.dart';

// ─── Infraestructura & Almacenamiento ─────────────────────────────────────────

final secureStorageServiceProvider = Provider<SecureStorageService>((ref) {
  return SecureStorageService();
});

final apiClientProvider = Provider<ApiClient>((ref) {
  final storage = ref.watch(secureStorageServiceProvider);
  return ApiClient(
    storageService: storage,
  );
});

// ─── Data Sources ─────────────────────────────────────────────────────────────

final authRemoteDataSourceProvider = Provider<AuthRemoteDataSource>((ref) {
  final apiClient = ref.watch(apiClientProvider);
  return AuthRemoteDataSourceImpl(apiClient);
});

final authLocalDataSourceProvider = Provider<AuthLocalDataSource>((ref) {
  final storage = ref.watch(secureStorageServiceProvider);
  return AuthLocalDataSourceImpl(storage);
});

// ─── Repositorios ─────────────────────────────────────────────────────────────

final authRepositoryProvider = Provider<AuthRepository>((ref) {
  final remote = ref.watch(authRemoteDataSourceProvider);
  final local = ref.watch(authLocalDataSourceProvider);
  return AuthRepositoryImpl(
    remoteDataSource: remote,
    localDataSource: local,
  );
});

// ─── Casos de Uso ─────────────────────────────────────────────────────────────

final loginWithGoogleUseCaseProvider = Provider<LoginWithGoogleUseCase>((ref) {
  final repository = ref.watch(authRepositoryProvider);
  return LoginWithGoogleUseCase(repository);
});

final loginWithLocalUseCaseProvider = Provider<LoginWithLocalUseCase>((ref) {
  final repository = ref.watch(authRepositoryProvider);
  return LoginWithLocalUseCase(repository);
});

final checkAuthStatusUseCaseProvider = Provider<CheckAuthStatusUseCase>((ref) {
  final repository = ref.watch(authRepositoryProvider);
  return CheckAuthStatusUseCase(repository);
});

final logoutUseCaseProvider = Provider<LogoutUseCase>((ref) {
  final repository = ref.watch(authRepositoryProvider);
  return LogoutUseCase(repository);
});

// ─── Notifier Principal de Estado de Auth ─────────────────────────────────────

final authNotifierProvider = StateNotifierProvider<AuthNotifier, AuthState>((ref) {
  return AuthNotifier(
    loginWithGoogleUseCase: ref.watch(loginWithGoogleUseCaseProvider),
    loginWithLocalUseCase: ref.watch(loginWithLocalUseCaseProvider),
    checkAuthStatusUseCase: ref.watch(checkAuthStatusUseCaseProvider),
    logoutUseCase: ref.watch(logoutUseCaseProvider),
  );
});
