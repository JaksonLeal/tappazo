import 'package:flutter/material.dart';

import '../../../domain/entities/game_event.dart';
import '../../theme/colors.dart';
import '../../theme/spacing.dart';
import '../../theme/typography.dart';

/// Aviso no intrusivo de conectividad: sin internet o reconectando STOMP.
/// Se oculta solo cuando todo vuelve a estar en línea.
class ConnectionBanner extends StatelessWidget {
  final bool isOnline;
  final RealtimeConnectionStatus connection;
  final VoidCallback? onRetry;

  const ConnectionBanner({
    super.key,
    required this.isOnline,
    required this.connection,
    this.onRetry,
  });

  @override
  Widget build(BuildContext context) {
    final (visible, color, icon, text, showSpinner, canRetry) = switch ((isOnline, connection)) {
      (false, _) => (
          true,
          AppColors.error,
          Icons.wifi_off_rounded,
          'Sin conexión a internet. Reconectaremos automáticamente.',
          false,
          false,
        ),
      (true, RealtimeConnectionStatus.reconnecting) => (
          true,
          AppColors.primary,
          Icons.sync_rounded,
          'Reconectando con la mesa…',
          true,
          false,
        ),
      (true, RealtimeConnectionStatus.disconnected) => (
          true,
          AppColors.error,
          Icons.cloud_off_rounded,
          'Sin conexión en tiempo real.',
          false,
          true,
        ),
      _ => (false, AppColors.primary, Icons.check, '', false, false),
    };

    return AnimatedSwitcher(
      duration: const Duration(milliseconds: 250),
      transitionBuilder: (child, animation) => SizeTransition(sizeFactor: animation, child: child),
      child: !visible
          ? const SizedBox(width: double.infinity, key: ValueKey('hidden'))
          : Container(
              key: ValueKey(text),
              width: double.infinity,
              padding: const EdgeInsets.symmetric(horizontal: AppSpacing.margin, vertical: AppSpacing.sm),
              color: color.withValues(alpha: 0.14),
              child: Row(
                children: [
                  if (showSpinner)
                    SizedBox(
                      width: 16,
                      height: 16,
                      child: CircularProgressIndicator(strokeWidth: 2, color: color),
                    )
                  else
                    Icon(icon, size: 18, color: color),
                  const SizedBox(width: AppSpacing.sm),
                  Expanded(child: Text(text, style: AppTypography.bodySm.copyWith(color: color))),
                  if (canRetry && onRetry != null)
                    TextButton(
                      onPressed: onRetry,
                      style: TextButton.styleFrom(foregroundColor: color, visualDensity: VisualDensity.compact),
                      child: const Text('Reintentar'),
                    ),
                ],
              ),
            ),
    );
  }
}
