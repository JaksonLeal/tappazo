import 'package:flutter/material.dart';

import '../../../domain/entities/game_event.dart';
import '../../theme/colors.dart';
import '../../theme/spacing.dart';
import '../../theme/typography.dart';

/// Pill que refleja el estado de la conexión STOMP.
class ConnectionBadge extends StatelessWidget {
  final RealtimeConnectionStatus status;
  final VoidCallback? onRetry;

  const ConnectionBadge({super.key, required this.status, this.onRetry});

  @override
  Widget build(BuildContext context) {
    final (label, color) = switch (status) {
      RealtimeConnectionStatus.connected => ('EN VIVO', AppColors.secondary),
      RealtimeConnectionStatus.connecting => ('CONECTANDO', AppColors.primary),
      RealtimeConnectionStatus.reconnecting => ('RECONECTANDO', AppColors.primary),
      RealtimeConnectionStatus.disconnected => ('SIN CONEXIÓN', AppColors.error),
    };
    final canRetry = status == RealtimeConnectionStatus.disconnected && onRetry != null;

    return Semantics(
      label: 'Estado de conexión: $label',
      button: canRetry,
      child: GestureDetector(
        onTap: canRetry ? onRetry : null,
        child: Container(
          padding: const EdgeInsets.symmetric(horizontal: 10, vertical: 4),
          decoration: BoxDecoration(
            color: color.withValues(alpha: 0.12),
            borderRadius: AppSpacing.borderRadiusFull,
            border: Border.all(color: color.withValues(alpha: 0.6)),
          ),
          child: Row(
            mainAxisSize: MainAxisSize.min,
            children: [
              Container(
                width: 8,
                height: 8,
                decoration: BoxDecoration(color: color, shape: BoxShape.circle),
              ),
              const SizedBox(width: 6),
              Text(label, style: AppTypography.labelSm.copyWith(color: color)),
              if (canRetry) ...[
                const SizedBox(width: 4),
                Icon(Icons.refresh_rounded, size: 14, color: color),
              ],
            ],
          ),
        ),
      ),
    );
  }
}
