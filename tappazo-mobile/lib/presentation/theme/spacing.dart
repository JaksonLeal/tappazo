import 'package:flutter/material.dart';

/// Tokens de espaciado, radios y tamaños de toque de Tappazo.
/// Única fuente de verdad: tappazo-mobile/design/design_tokens.yaml
class AppSpacing {
  AppSpacing._();

  // Espaciados
  static const double xs = 4.0;
  static const double sm = 8.0;
  static const double md = 16.0;
  static const double lg = 24.0;
  static const double xl = 32.0;
  static const double margin = 20.0;
  static const double gutter = 16.0;

  // Radios de borde
  static const double radiusSm = 8.0;
  static const double radiusMd = 12.0;
  static const double radiusFull = 9999.0;

  static final BorderRadius borderRadiusSm = BorderRadius.circular(radiusSm);
  static final BorderRadius borderRadiusMd = BorderRadius.circular(radiusMd);
  static final BorderRadius borderRadiusFull = BorderRadius.circular(radiusFull);

  // Dimensiones de interacción táctil
  static const double buttonPrimaryHeight = 56.0;
  static const double avatarSize = 48.0;
  static const double iconButtonSize = 44.0;
}

