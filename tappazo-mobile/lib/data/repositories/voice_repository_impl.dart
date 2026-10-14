import '../../domain/entities/voice_entities.dart';
import '../../domain/repositories/voice_repository.dart';
import '../datasources/voice_remote_datasource.dart';

class VoiceRepositoryImpl implements VoiceRepository {
  final VoiceRemoteDataSource _remote;
  VoiceRepositoryImpl(this._remote);

  @override
  Future<VoiceToken> fetchVoiceToken(String gameId) => _remote.fetchVoiceToken(gameId);
}
