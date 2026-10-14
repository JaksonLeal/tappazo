enum AttemptStatus {
  pending('PENDING'),
  approved('APPROVED'),
  rejected('REJECTED');

  final String apiValue;
  const AttemptStatus(this.apiValue);

  static AttemptStatus fromString(String? value) {
    for (final s in AttemptStatus.values) {
      if (s.apiValue == value) return s;
    }
    return AttemptStatus.pending;
  }
}

/// Intento de revelación (Sección 24). Cada voto referencia un attemptId concreto.
class RevealAttemptEntity {
  final String id;
  final String roundId;
  final String revealId;
  final int attemptNumber;
  final int proposedNumber;

  /// fileKey en el bucket. `null` si se omitió la foto (falla de cámara).
  final String? photoReference;
  final AttemptStatus status;

  /// Revelador, si el backend lo envía (ATTEMPT_SUBMITTED aún no lo incluye).
  final String? playerId;
  final DateTime? receivedAt;

  const RevealAttemptEntity({
    required this.id,
    required this.roundId,
    required this.revealId,
    required this.attemptNumber,
    required this.proposedNumber,
    this.photoReference,
    this.status = AttemptStatus.pending,
    this.playerId,
    this.receivedAt,
  });

  static const int maxAttempts = 3;

  bool get hasPhoto => photoReference != null && photoReference!.isNotEmpty;
  bool get isPending => status == AttemptStatus.pending;
}
