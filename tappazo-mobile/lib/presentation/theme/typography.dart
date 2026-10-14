import 'package:flutter/material.dart';
import 'package:google_fonts/google_fonts.dart';
import 'colors.dart';

/// Tokens de tipografía oficiales de Tappazo.
/// Única fuente de verdad: tappazo-mobile/design/design_tokens.yaml
class AppTypography {
  AppTypography._();

  static const String fontFamily = 'Outfit';

  // Display / Tapa / Moneda
  static TextStyle get capDisplay => GoogleFonts.outfit(
        fontSize: 44,
        height: 1.0,
        fontWeight: FontWeight.w900,
        letterSpacing: -1.32,
        color: AppColors.textPrimary,
      );

  static TextStyle get currencyDisplay => GoogleFonts.outfit(
        fontSize: 26,
        height: 32 / 26,
        fontWeight: FontWeight.w800,
        letterSpacing: -0.26,
        color: AppColors.textPrimary,
      );

  // Headlines
  static TextStyle get headlineXl => GoogleFonts.outfit(
        fontSize: 40,
        height: 48 / 40,
        fontWeight: FontWeight.w800,
        letterSpacing: -0.8,
        color: AppColors.textPrimary,
      );

  static TextStyle get headlineXlMobile => GoogleFonts.outfit(
        fontSize: 32,
        height: 40 / 32,
        fontWeight: FontWeight.w800,
        letterSpacing: -0.64,
        color: AppColors.textPrimary,
      );

  static TextStyle get headlineLg => GoogleFonts.outfit(
        fontSize: 28,
        height: 36 / 28,
        fontWeight: FontWeight.w700,
        letterSpacing: -0.28,
        color: AppColors.textPrimary,
      );

  static TextStyle get headlineMd => GoogleFonts.outfit(
        fontSize: 22,
        height: 28 / 22,
        fontWeight: FontWeight.w700,
        color: AppColors.textPrimary,
      );

  static TextStyle get headlineSm => GoogleFonts.outfit(
        fontSize: 18,
        height: 24 / 18,
        fontWeight: FontWeight.w600,
        color: AppColors.textPrimary,
      );

  // Body
  static TextStyle get bodyLg => GoogleFonts.outfit(
        fontSize: 16,
        height: 24 / 16,
        fontWeight: FontWeight.w400,
        color: AppColors.textPrimary,
      );

  static TextStyle get bodyMd => GoogleFonts.outfit(
        fontSize: 14,
        height: 20 / 14,
        fontWeight: FontWeight.w400,
        color: AppColors.textPrimary,
      );

  static TextStyle get bodySm => GoogleFonts.outfit(
        fontSize: 12,
        height: 16 / 12,
        fontWeight: FontWeight.w400,
        color: AppColors.textSecondary,
      );

  // Labels
  static TextStyle get labelLg => GoogleFonts.outfit(
        fontSize: 16,
        height: 20 / 16,
        fontWeight: FontWeight.w700,
        letterSpacing: 0.32,
        color: AppColors.textPrimary,
      );

  static TextStyle get labelMd => GoogleFonts.outfit(
        fontSize: 14,
        height: 18 / 14,
        fontWeight: FontWeight.w600,
        letterSpacing: 0.14,
        color: AppColors.textPrimary,
      );

  static TextStyle get labelSm => GoogleFonts.outfit(
        fontSize: 11,
        height: 14 / 11,
        fontWeight: FontWeight.w600,
        letterSpacing: 0.55,
        color: AppColors.textSecondary,
      );
}

