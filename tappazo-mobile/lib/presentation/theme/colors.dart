import 'package:flutter/material.dart';

/// Tokens de color oficiales de Tappazo.
/// Única fuente de verdad: tappazo-mobile/design/design_tokens.yaml
class AppColors {
  AppColors._();

  // Fondos y superficies
  static const Color background = Color(0xFF12131A);
  static const Color surface = Color(0xFF12131A);
  static const Color surfaceContainerLowest = Color(0xFF0D0E14);
  static const Color surfaceContainerLow = Color(0xFF1A1B22);
  static const Color surfaceContainer = Color(0xFF1E1F26);
  static const Color surfaceContainerHigh = Color(0xFF292931);
  static const Color surfaceContainerHighest = Color(0xFF33343C);
  static const Color surfaceBright = Color(0xFF383940);

  // Acentos semánticos
  static const Color primary = Color(0xFFFFB300); // Ámbar / botón principal
  static const Color onPrimaryContainer = Color(0xFF6B4900); // Texto sobre botón ámbar
  static const Color secondary = Color(0xFFBEF500); // Lima / aprobación / quórum
  static const Color onSecondary = Color(0xFF273500);
  static const Color error = Color(0xFFFFB4AB); // Error suave
  static const Color errorContainer = Color(0xFF93000A); // Error fuerte

  // Texto y bordes
  static const Color textPrimary = Color(0xFFE3E1EB);
  static const Color textSecondary = Color(0xFFD6C4AC);
  static const Color outline = Color(0xFF9E8E78);
}

