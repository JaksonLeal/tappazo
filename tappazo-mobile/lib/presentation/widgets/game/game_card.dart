import 'package:flutter/material.dart';

import '../../theme/colors.dart';
import '../../theme/spacing.dart';
import '../../theme/typography.dart';

/// Tarjeta base de superficie oscura usada en las pantallas de juego.
class GameCard extends StatelessWidget {
  final Widget child;
  final Color? borderColor;
  final Color? color;
  final EdgeInsetsGeometry padding;

  const GameCard({
    super.key,
    required this.child,
    this.borderColor,
    this.color,
    this.padding = const EdgeInsets.all(AppSpacing.md),
  });

  @override
  Widget build(BuildContext context) {
    return Container(
      padding: padding,
      decoration: BoxDecoration(
        color: color ?? AppColors.surfaceContainer,
        borderRadius: AppSpacing.borderRadiusMd,
        border: Border.all(color: borderColor ?? AppColors.surfaceContainerHighest),
      ),
      child: child,
    );
  }
}

/// Pill de estado (badge) con color semántico.
class StatusPill extends StatelessWidget {
  final String label;
  final Color color;
  final IconData? icon;

  const StatusPill({super.key, required this.label, required this.color, this.icon});

  @override
  Widget build(BuildContext context) {
    return Container(
      padding: const EdgeInsets.symmetric(horizontal: 10, vertical: 4),
      decoration: BoxDecoration(
        color: color.withValues(alpha: 0.14),
        borderRadius: AppSpacing.borderRadiusFull,
      ),
      child: Row(
        mainAxisSize: MainAxisSize.min,
        children: [
          if (icon != null) ...[
            Icon(icon, size: 14, color: color),
            const SizedBox(width: 4),
          ],
          Text(label, style: AppTypography.labelSm.copyWith(color: color)),
        ],
      ),
    );
  }
}

/// Tapa circular con el número en tipografía cap_display.
class CapNumberBadge extends StatelessWidget {
  final int? number;
  final double size;
  final Color color;

  const CapNumberBadge({
    super.key,
    required this.number,
    this.size = 88,
    this.color = AppColors.primary,
  });

  @override
  Widget build(BuildContext context) {
    return Container(
      width: size,
      height: size,
      alignment: Alignment.center,
      decoration: BoxDecoration(
        shape: BoxShape.circle,
        color: AppColors.surfaceContainerLowest,
        border: Border.all(color: color, width: 3),
        boxShadow: [BoxShadow(color: color.withValues(alpha: 0.25), blurRadius: 18)],
      ),
      child: FittedBox(
        child: Padding(
          padding: const EdgeInsets.all(AppSpacing.sm),
          child: Text(
            number?.toString().padLeft(2, '0') ?? '--',
            style: AppTypography.capDisplay.copyWith(color: color),
          ),
        ),
      ),
    );
  }
}
