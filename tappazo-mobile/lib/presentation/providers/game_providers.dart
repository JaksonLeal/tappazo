import 'package:flutter_riverpod/flutter_riverpod.dart';
import '../../data/datasources/game_remote_datasource.dart';
import '../../data/repositories/game_repository_impl.dart';
import '../../domain/repositories/game_repository.dart';
import '../../domain/usecases/create_game_usecase.dart';
import '../../domain/usecases/get_game_details_usecase.dart';
import '../../domain/usecases/join_game_usecase.dart';
import '../../domain/usecases/update_game_settings_usecase.dart';
import 'auth_providers.dart';
import 'game_notifier.dart';
import 'game_state.dart';

// ─── Data Source ─────────────────────────────────────────────────────────────

final gameRemoteDataSourceProvider = Provider<GameRemoteDataSource>((ref) {
  final apiClient = ref.watch(apiClientProvider);
  return GameRemoteDataSourceImpl(apiClient);
});

// ─── Repositorio ─────────────────────────────────────────────────────────────

final gameRepositoryProvider = Provider<GameRepository>((ref) {
  final remote = ref.watch(gameRemoteDataSourceProvider);
  return GameRepositoryImpl(remote);
});

// ─── Casos de Uso ─────────────────────────────────────────────────────────────

final createGameUseCaseProvider = Provider<CreateGameUseCase>((ref) {
  final repo = ref.watch(gameRepositoryProvider);
  return CreateGameUseCase(repo);
});

final joinGameUseCaseProvider = Provider<JoinGameUseCase>((ref) {
  final repo = ref.watch(gameRepositoryProvider);
  return JoinGameUseCase(repo);
});

final getGameDetailsUseCaseProvider = Provider<GetGameDetailsUseCase>((ref) {
  final repo = ref.watch(gameRepositoryProvider);
  return GetGameDetailsUseCase(repo);
});

final updateGameSettingsUseCaseProvider = Provider<UpdateGameSettingsUseCase>((ref) {
  final repo = ref.watch(gameRepositoryProvider);
  return UpdateGameSettingsUseCase(repo);
});

// ─── Game Notifier Principal ─────────────────────────────────────────────────

final gameNotifierProvider = StateNotifierProvider<GameNotifier, GameStateData>((ref) {
  return GameNotifier(
    createGameUseCase: ref.watch(createGameUseCaseProvider),
    joinGameUseCase: ref.watch(joinGameUseCaseProvider),
    getGameDetailsUseCase: ref.watch(getGameDetailsUseCaseProvider),
    updateGameSettingsUseCase: ref.watch(updateGameSettingsUseCaseProvider),
  );
});

