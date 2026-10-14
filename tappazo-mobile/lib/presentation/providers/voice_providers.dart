import 'package:flutter_riverpod/flutter_riverpod.dart';

import '../../data/datasources/voice_remote_datasource.dart';
import '../../data/repositories/voice_repository_impl.dart';
import '../../data/services/livekit_voice_call_service.dart';
import '../../domain/repositories/voice_repository.dart';
import '../../domain/services/voice_call_service.dart';
import '../../domain/usecases/get_voice_token_usecase.dart';
import 'auth_providers.dart';
import 'game_session_providers.dart';
import 'voice_notifier.dart';

final voiceRemoteDataSourceProvider = Provider<VoiceRemoteDataSource>((ref) {
  return VoiceRemoteDataSourceImpl(ref.watch(apiClientProvider));
});

final voiceRepositoryProvider = Provider<VoiceRepository>((ref) {
  return VoiceRepositoryImpl(ref.watch(voiceRemoteDataSourceProvider));
});

final getVoiceTokenUseCaseProvider = Provider<GetVoiceTokenUseCase>((ref) {
  return GetVoiceTokenUseCase(ref.watch(voiceRepositoryProvider));
});

/// Fábrica del cliente de voz (sobrescribible en tests).
final voiceCallServiceFactoryProvider = Provider<VoiceCallService Function()>((ref) {
  return LiveKitVoiceCallService.new;
});

/// Llamada de voz de la partida [gameId]. Se cuelga al salir de la pantalla.
final voiceProvider = StateNotifierProvider.autoDispose.family<VoiceNotifier, VoiceState, String>((ref, gameId) {
  return VoiceNotifier(
    gameId: gameId,
    getToken: ref.watch(getVoiceTokenUseCaseProvider),
    service: ref.watch(voiceCallServiceFactoryProvider)(),
    networkChanges: ref.watch(networkMonitorProvider).watchOnline(),
  );
});
