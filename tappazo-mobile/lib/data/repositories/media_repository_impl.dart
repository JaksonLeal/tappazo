import '../../core/network/error_messages.dart';
import '../../domain/repositories/media_repository.dart';
import '../datasources/media_remote_datasource.dart';

/// Fotos de revelación (Sección 26): el backend firma, el cliente sube directo
/// al bucket S3/MinIO y solo la referencia (`fileKey`) viaja en el intento.
class MediaRepositoryImpl implements MediaRepository {
  final MediaRemoteDataSource _remote;
  final Duration retryDelay;
  final Map<String, String> _urlCache = {};

  MediaRepositoryImpl(this._remote, {this.retryDelay = const Duration(milliseconds: 800)});

  static const String _revealFolder = 'reveals';
  static const String _jpeg = 'image/jpeg';

  @override
  Future<String> uploadRevealPhoto(List<int> jpegBytes) async {
    // 1. POST /api/v1/media/presigned-upload
    final presigned = await _withRetry(() => _remote.requestUpload(
          fileName: 'reveal-${DateTime.now().millisecondsSinceEpoch}.jpg',
          contentType: _jpeg,
          folder: _revealFolder,
        ));
    // 2. PUT directo al bucket con la URL prefirmada (válida 15 min).
    await _withRetry(() => _remote.uploadBytes(
          uploadUrl: presigned.uploadUrl,
          bytes: jpegBytes,
          contentType: _jpeg,
        ));
    if (presigned.downloadUrl != null) {
      _urlCache[presigned.fileKey] = presigned.downloadUrl!;
    }
    // 3. El fileKey es el photoReference del RevealAttempt.
    return presigned.fileKey;
  }

  @override
  Future<String> resolvePhotoUrl(String fileKey) async {
    final cached = _urlCache[fileKey];
    if (cached != null) return cached;
    final url = await _withRetry(() => _remote.getDownloadUrl(fileKey));
    _urlCache[fileKey] = url;
    return url;
  }

  /// Un reintento ante fallos transitorios de red (redes móviles inestables).
  Future<T> _withRetry<T>(Future<T> Function() action) async {
    try {
      return await action();
    } catch (e) {
      if (!isTransientNetworkError(e)) rethrow;
      await Future<void>.delayed(retryDelay);
      return action();
    }
  }
}
