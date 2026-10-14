import 'package:flutter_riverpod/flutter_riverpod.dart';

import '../../core/constants/api_constants.dart';
import '../../data/datasources/game_realtime_datasource.dart';
import '../../data/datasources/media_remote_datasource.dart';
import '../../data/datasources/stomp/stomp_client.dart';
import '../../data/repositories/game_realtime_repository_impl.dart';
import '../../data/repositories/media_repository_impl.dart';
import '../../domain/repositories/game_realtime_repository.dart';
import '../../domain/repositories/media_repository.dart';
import '../../domain/services/cap_number_recognizer.dart';
import '../../domain/usecases/cast_vote_usecases.dart';
import '../../domain/usecases/submit_reveal_attempt_usecase.dart';
import 'auth_providers.dart';
import 'auth_state.dart';
import 'game_providers.dart';
import 'game_session_notifier.dart';
import 'game_session_state.dart';
import 'game_state.dart';

// ─── Tiempo real (STOMP) ─────────────────────────────────────────────────────

final stompClientProvider = Provider<StompClient>((ref) {
  final storage = ref.watch(secureStorageServiceProvider);
  final client = StompClient(
    url: ApiConstants.wsUrl,
    tokenProvider: storage.getToken,
  );
  ref.onDispose(client.dispose);
  return client;
});

final gameRealtimeDataSourceProvider = Provider<GameRealtimeDataSource>((ref) {
  return GameRealtimeDataSourceImpl(ref.watch(stompClientProvider));
});

final gameRealtimeRepositoryProvider = Provider<GameRealtimeRepository>((ref) {
  return GameRealtimeRepositoryImpl(ref.watch(gameRealtimeDataSourceProvider));
});

// ─── Media (fotos de revelación) ─────────────────────────────────────────────

final mediaRemoteDataSourceProvider = Provider<MediaRemoteDataSource>((ref) {
  return MediaRemoteDataSourceImpl(ref.watch(apiClientProvider));
});

final mediaRepositoryProvider = Provider<MediaRepository>((ref) {
  return MediaRepositoryImpl(ref.watch(mediaRemoteDataSourceProvider));
});

/// URL temporal para una foto de revelación (fileKey → presigned GET).
final revealPhotoUrlProvider = FutureProvider.autoDispose.family<String, String>((ref, fileKey) {
  return ref.watch(mediaRepositoryProvider).resolvePhotoUrl(fileKey);
});

/// Asistente OCR local (Sección 20). Sustituible por una implementación ML Kit.
final capNumberRecognizerProvider = Provider<CapNumberRecognizer>((ref) {
  return const ManualCapNumberRecognizer();
});

// ─── Casos de uso ────────────────────────────────────────────────────────────

final submitRevealAttemptUseCaseProvider = Provider<SubmitRevealAttemptUseCase>((ref) {
  return SubmitRevealAttemptUseCase(
    ref.watch(gameRealtimeRepositoryProvider),
    ref.watch(mediaRepositoryProvider),
  );
});

final castTieBreakVoteUseCaseProvider = Provider<CastTieBreakVoteUseCase>((ref) {
  return CastTieBreakVoteUseCase(ref.watch(gameRealtimeRepositoryProvider));
});

final castRevealVoteUseCaseProvider = Provider<CastRevealVoteUseCase>((ref) {
  return CastRevealVoteUseCase(ref.watch(gameRealtimeRepositoryProvider));
});

// ─── Sesión de juego por partida ─────────────────────────────────────────────

final currentUserIdProvider = Provider<String>((ref) {
  final auth = ref.watch(authNotifierProvider);
  return auth is Authenticated ? auth.user.id : '';
});

/// Sesión en tiempo real de la partida [gameId]. Se mantiene viva mientras
/// la sala o la pantalla de juego estén montadas y se desconecta al salir.
final gameSessionProvider =
    StateNotifierProvider.autoDispose.family<GameSessionNotifier, GameSessionState, String>((ref, gameId) {
  final notifier = GameSessionNotifier(
    gameId: gameId,
    currentUserId: ref.read(currentUserIdProvider),
    realtime: ref.watch(gameRealtimeRepositoryProvider),
    submitAttempt: ref.watch(submitRevealAttemptUseCaseProvider),
    castTieBreakVote: ref.watch(castTieBreakVoteUseCaseProvider),
    castRevealVote: ref.watch(castRevealVoteUseCaseProvider),
    resync: () async {
      await ref.read(gameNotifierProvider.notifier).refreshGameDetails(gameId);
      final gameState = ref.read(gameNotifierProvider);
      return gameState is GameLoaded && gameState.game.id == gameId ? gameState.details : null;
    },
  );
  notifier.start();
  return notifier;
});
