import '../entities/game_decisions.dart';
import '../repositories/game_realtime_repository.dart';
import '../repositories/media_repository.dart';

class RevealAttemptValidationException implements Exception {
  final String message;
  const RevealAttemptValidationException(this.message);
  @override
  String toString() => message;
}

/// Envía un RevealAttempt (Secciones 19, 21, 26):
/// 1. Valida número 0-99 y que haya foto o un motivo de omisión por fallo de cámara.
/// 2. Sube la foto con URL prefirmada.
/// 3. Envía el intento por STOMP a `/app/game/{gameId}/attempt`.
///
/// Devuelve el `photoReference` enviado (o `null` si se omitió la foto) para
/// poder reconocer el eco ATTEMPT_SUBMITTED propio.
class SubmitRevealAttemptUseCase {
  final GameRealtimeRepository _realtime;
  final MediaRepository _media;

  SubmitRevealAttemptUseCase(this._realtime, this._media);

  static const int minNumber = 0;
  static const int maxNumber = 99;

  Future<String?> execute({
    required String gameId,
    required String roundId,
    required int proposedNumber,
    List<int>? photoBytes,
    PhotoOmittedReason? photoOmittedReason,
    void Function()? onUploaded,
  }) async {
    if (proposedNumber < minNumber || proposedNumber > maxNumber) {
      throw const RevealAttemptValidationException('El número debe estar entre 0 y 99.');
    }
    final hasPhoto = photoBytes != null && photoBytes.isNotEmpty;
    if (!hasPhoto && photoOmittedReason == null) {
      throw const RevealAttemptValidationException('La foto de la tapa es obligatoria.');
    }

    String? photoReference;
    if (hasPhoto) {
      photoReference = await _media.uploadRevealPhoto(photoBytes);
      onUploaded?.call();
    }

    _realtime.submitAttempt(
      gameId: gameId,
      roundId: roundId,
      proposedNumber: proposedNumber,
      photoReference: photoReference,
      photoOmittedReason: hasPhoto ? null : photoOmittedReason,
    );
    return photoReference;
  }
}
