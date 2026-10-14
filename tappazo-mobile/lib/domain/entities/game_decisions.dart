/// Decisión de voto sobre un RevealAttempt (backend `Decision`: YES / NO).
enum RevealVoteDecision {
  yes('YES', 'SÍ'),
  no('NO', 'NO');

  final String apiValue;
  final String label;
  const RevealVoteDecision(this.apiValue, this.label);
}

/// Opciones de desempate (Sección 33).
enum TieBreakDecision {
  nuevaRonda('NUEVA_RONDA', 'Nueva ronda', 'Los empatados vuelven a destapar para definir los puestos restantes.'),
  dividirCuenta('DIVIDIR_CUENTA', 'Dividir cuenta', 'Se reparte la cuenta entre los empatados según las reglas económicas.');

  final String apiValue;
  final String label;
  final String description;
  const TieBreakDecision(this.apiValue, this.label, this.description);

  static TieBreakDecision? fromString(String? value) {
    for (final d in TieBreakDecision.values) {
      if (d.apiValue == value) return d;
    }
    return null;
  }
}

/// Motivos válidos para omitir la foto (Sección 21): solo fallos de cámara.
enum PhotoOmittedReason {
  cameraUnavailable('CAMERA_UNAVAILABLE', 'La cámara no está disponible en este dispositivo'),
  cameraPermissionDenied('CAMERA_PERMISSION_DENIED', 'Permiso de cámara denegado'),
  cameraError('CAMERA_ERROR', 'La cámara falló al tomar la foto');

  final String apiValue;
  final String description;
  const PhotoOmittedReason(this.apiValue, this.description);
}
