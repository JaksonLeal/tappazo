/// Asistente local de reconocimiento del número de la tapa (Sección 20).
///
/// Es solo una sugerencia: el jugador siempre confirma o corrige el número
/// antes de enviarlo, y el backend/votación es quien lo valida.
abstract class CapNumberRecognizer {
  /// Devuelve un número 0-99 sugerido, o `null` si no hay sugerencia.
  Future<int?> suggestNumber(String imagePath);

  /// `false` si no hay motor OCR instalado (la UI lo indica al usuario).
  bool get isAvailable;
}

/// Implementación por defecto sin motor OCR: no sugiere números.
/// Sustituir por una basada en ML Kit / TFLite sin tocar la UI.
class ManualCapNumberRecognizer implements CapNumberRecognizer {
  const ManualCapNumberRecognizer();

  @override
  bool get isAvailable => false;

  @override
  Future<int?> suggestNumber(String imagePath) async => null;
}
