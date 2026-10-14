import 'package:dio/dio.dart';

import '../../core/constants/api_constants.dart';
import '../../core/network/api_client.dart';
import '../../domain/entities/voice_entities.dart';

abstract class VoiceRemoteDataSource {
  /// POST /api/v1/games/{id}/voice-token
  Future<VoiceToken> fetchVoiceToken(String gameId);
}

class VoiceRemoteDataSourceImpl implements VoiceRemoteDataSource {
  final ApiClient _apiClient;

  VoiceRemoteDataSourceImpl(this._apiClient);

  @override
  Future<VoiceToken> fetchVoiceToken(String gameId) async {
    final response = await _apiClient.dio.post(ApiConstants.voiceToken(gameId));
    final data = response.data;
    if (response.statusCode == 200 && data is Map<String, dynamic>) {
      final token = data['token'] as String?;
      final roomName = data['roomName'] as String?;
      final serverUrl = data['serverUrl'] as String?;
      if (token != null && token.isNotEmpty && serverUrl != null && serverUrl.isNotEmpty) {
        return VoiceToken(
          token: token,
          roomName: roomName ?? 'game-$gameId',
          serverUrl: ApiConstants.liveKitUrlOverride.isNotEmpty ? ApiConstants.liveKitUrlOverride : serverUrl,
        );
      }
    }
    throw DioException(
      requestOptions: response.requestOptions,
      response: response,
      message: 'Respuesta inesperada al solicitar el token de voz',
    );
  }
}
