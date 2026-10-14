class PresignedUpload {
  final String uploadUrl;
  final String fileKey;
  final String? downloadUrl;

  const PresignedUpload({required this.uploadUrl, required this.fileKey, this.downloadUrl});
}

/// Almacenamiento de fotos de revelación (Sección 26): subida por URL prefirmada.
abstract class MediaRepository {
  /// Sube la foto y devuelve el `fileKey` a usar como `photoReference`.
  Future<String> uploadRevealPhoto(List<int> jpegBytes);

  /// URL temporal para visualizar una foto protegida.
  Future<String> resolvePhotoUrl(String fileKey);
}
