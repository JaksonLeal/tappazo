import 'package:flutter/material.dart';

import '../../../core/utils/currency_formatter.dart';
import '../../../domain/entities/round_outcome_entities.dart';
import '../../theme/colors.dart';
import '../../theme/spacing.dart';
import '../../theme/typography.dart';
import 'game_card.dart';

/// Resultado de la ronda tal como lo publicó el backend (ROUND_RESOLVED).
class RoundResolutionPanel extends StatelessWidget {
  final RoundResolutionEntity resolution;
  final String currentUserId;
  final String Function(String userId) nameOf;

  const RoundResolutionPanel({
    super.key,
    required this.resolution,
    required this.currentUserId,
    required this.nameOf,
  });

  String _label(String id) => id == currentUserId ? 'Tú' : nameOf(id);

  @override
  Widget build(BuildContext context) {
    final iLost = resolution.confirmedLoserIds.contains(currentUserId);
    final iWon = resolution.winnerIds.contains(currentUserId);

    return Column(
      crossAxisAlignment: CrossAxisAlignment.stretch,
      children: [
        GameCard(
          borderColor: (iLost ? AppColors.error : AppColors.secondary).withValues(alpha: 0.6),
          child: Column(
            children: [
              Icon(
                iLost ? Icons.local_bar_rounded : Icons.emoji_events_rounded,
                size: 40,
                color: iLost ? AppColors.error : AppColors.secondary,
              ),
              const SizedBox(height: AppSpacing.sm),
              Text(
                iLost ? 'TE TOCA PAGAR' : (iWon ? '¡TE SALVASTE!' : 'RONDA RESUELTA'),
                style: AppTypography.headlineLg.copyWith(
                  color: iLost ? AppColors.error : AppColors.secondary,
                ),
              ),
              const SizedBox(height: AppSpacing.xs),
              Text('Resultado oficial del servidor', style: AppTypography.bodySm),
            ],
          ),
        ),
        const SizedBox(height: AppSpacing.lg),
        _section(
          'PAGAN',
          AppColors.error,
          Icons.local_bar_rounded,
          resolution.confirmedLoserIds,
          empty: 'Sin perdedores confirmados',
        ),
        const SizedBox(height: AppSpacing.md),
        _section(
          'SE SALVAN',
          AppColors.secondary,
          Icons.check_circle_rounded,
          resolution.winnerIds,
          empty: 'Sin ganadores',
        ),
        if (resolution.debtMovements.isNotEmpty) ...[
          const SizedBox(height: AppSpacing.lg),
          Text('MOVIMIENTOS DE DEUDA', style: AppTypography.labelSm),
          const SizedBox(height: AppSpacing.sm),
          GameCard(
            padding: const EdgeInsets.symmetric(vertical: AppSpacing.xs),
            child: Column(
              children: [
                for (final m in resolution.debtMovements)
                  ListTile(
                    dense: true,
                    leading: const Icon(Icons.arrow_forward_rounded, color: AppColors.outline),
                    title: Text(
                      '${_label(m.fromPlayerId)} → ${_label(m.toPlayerId)}',
                      style: AppTypography.labelMd,
                    ),
                    trailing: Text(
                      CurrencyFormatter.format(m.amount),
                      style: AppTypography.labelLg.copyWith(color: AppColors.primary),
                    ),
                  ),
              ],
            ),
          ),
        ],
      ],
    );
  }

  Widget _section(String title, Color color, IconData icon, List<String> ids, {required String empty}) {
    return Column(
      crossAxisAlignment: CrossAxisAlignment.start,
      children: [
        Text(title, style: AppTypography.labelSm.copyWith(color: color)),
        const SizedBox(height: AppSpacing.sm),
        if (ids.isEmpty)
          Text(empty, style: AppTypography.bodySm)
        else
          Wrap(
            spacing: AppSpacing.sm,
            runSpacing: AppSpacing.sm,
            children: [
              for (final id in ids) StatusPill(label: _label(id).toUpperCase(), color: color, icon: icon),
            ],
          ),
      ],
    );
  }
}
