import '../../domain/repositories/media_repository.dart';
import '../datasources/media_remote_datasource.dart';

class MediaRepositoryImpl implements MediaRepository {
  final MediaRemoteDataSource _remote;
  final Map<String, String> _urlCache = {};

  MediaRepositoryImpl(this._remote);

  static const String _revealFolder = 'reveals';
  static const String _jpeg = 'image/jpeg';

  @override
  Future<String> uploadRevealPhoto(List<int> jpegBytes) async {
    final presigned = await _remote.requestUpload(
      fileName: 'reveal-${DateTime.now().millisecondsSinceEpoch}.jpg',
      contentType: _jpeg,
      folder: _revealFolder,
    );
    await _remote.uploadBytes(uploadUrl: presigned.uploadUrl, bytes: jpegBytes, contentType: _jpeg);
    if (presigned.downloadUrl != null) {
      _urlCache[presigned.fileKey] = presigned.downloadUrl!;
    }
    return presigned.fileKey;
  }

  @override
  Future<String> resolvePhotoUrl(String fileKey) async {
    final cached = _urlCache[fileKey];
    if (cached != null) return cached;
    final url = await _remote.getDownloadUrl(fileKey);
    _urlCache[fileKey] = url;
    return url;
  }
}
