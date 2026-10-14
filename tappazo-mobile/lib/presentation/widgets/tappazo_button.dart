import 'package:flutter/material.dart';
import '../theme/colors.dart';
import '../theme/spacing.dart';
import '../theme/typography.dart';

class TappazoButton extends StatelessWidget {
  final String text;
  final VoidCallback? onPressed;
  final bool isLoading;
  final bool isOutlined;
  final IconData? icon;
  final Color? backgroundColor;
  final Color? textColor;

  const TappazoButton({
    super.key,
    required this.text,
    required this.onPressed,
    this.isLoading = false,
    this.isOutlined = false,
    this.icon,
    this.backgroundColor,
    this.textColor,
  });

  @override
  Widget build(BuildContext context) {
    if (isOutlined) {
      final effectiveColor = textColor ?? AppColors.textPrimary;
      return OutlinedButton(
        onPressed: isLoading ? null : onPressed,
        style: OutlinedButton.styleFrom(
          side: BorderSide(
            color: backgroundColor ?? AppColors.outline,
            width: 1.0,
          ),
          foregroundColor: effectiveColor,
          minimumSize: const Size.fromHeight(AppSpacing.buttonPrimaryHeight),
          shape: RoundedRectangleBorder(
            borderRadius: AppSpacing.borderRadiusMd,
          ),
        ),
        child: _buildChild(context, effectiveColor),
      );
    }

    final effectiveBg = backgroundColor ?? AppColors.primary;
    final effectiveFg = textColor ?? AppColors.onPrimaryContainer;

    return ElevatedButton(
      onPressed: isLoading ? null : onPressed,
      style: ElevatedButton.styleFrom(
        backgroundColor: effectiveBg,
        foregroundColor: effectiveFg,
        minimumSize: const Size.fromHeight(AppSpacing.buttonPrimaryHeight),
        shape: RoundedRectangleBorder(
          borderRadius: AppSpacing.borderRadiusMd,
        ),
        elevation: 0,
      ),
      child: _buildChild(context, effectiveFg),
    );
  }

  Widget _buildChild(BuildContext context, Color contentColor) {
    if (isLoading) {
      return SizedBox(
        height: 20,
        width: 20,
        child: CircularProgressIndicator(
          strokeWidth: 2.0,
          valueColor: AlwaysStoppedAnimation<Color>(contentColor),
        ),
      );
    }

    if (icon != null) {
      return Row(
        mainAxisAlignment: MainAxisAlignment.center,
        children: [
          Icon(icon, size: 20, color: contentColor),
          const SizedBox(width: AppSpacing.sm),
          Text(
            text,
            style: AppTypography.labelLg.copyWith(color: contentColor),
          ),
        ],
      );
    }

    return Text(
      text,
      style: AppTypography.labelLg.copyWith(color: contentColor),
    );
  }
}
