import 'package:flutter/material.dart';

import '../../../domain/entities/game_decisions.dart';
import '../../../domain/entities/round_outcome_entities.dart';
import '../../theme/colors.dart';
import '../../theme/spacing.dart';
import '../../theme/typography.dart';
import 'game_card.dart';

/// Interfaz de desempate (Secciones 33 y 70): los empatados votan
/// NUEVA_RONDA o DIVIDIR_CUENTA; ante desacuerdo decide el árbitro que
/// selecciona el backend (TIE_BREAK_DECIDER_SELECTED).
class TieBreakPanel extends StatelessWidget {
  final TieBreakEntity tieBreak;
  final String currentUserId;
  final String Function(String userId) nameOf;

  /// `true` si el usuario puede participar como empatado (UI; el backend valida).
  final bool canVoteAsTied;
  final TieBreakDecision? myVote;
  final TieBreakDecision? myArbiterVote;
  final bool isConnected;
  final void Function(TieBreakDecision decision, {required bool asArbiter}) onVote;

  const TieBreakPanel({
    super.key,
    required this.tieBreak,
    required this.currentUserId,
    required this.nameOf,
    required this.canVoteAsTied,
    required this.myVote,
    required this.myArbiterVote,
    required this.isConnected,
    required this.onVote,
  });

  bool get _iAmArbiter => tieBreak.hasDecider && tieBreak.selectedDeciderId == currentUserId;

  @override
  Widget build(BuildContext context) {
    return Column(
      crossAxisAlignment: CrossAxisAlignment.stretch,
      children: [
        _header(),
        const SizedBox(height: AppSpacing.lg),
        if (tieBreak.isResolved) _resolved() else ..._pending(),
      ],
    );
  }

  Widget _header() {
    return GameCard(
      borderColor: AppColors.primary.withValues(alpha: 0.6),
      child: Column(
        children: [
          const Icon(Icons.balance_rounded, color: AppColors.primary, size: 40),
          const SizedBox(height: AppSpacing.sm),
          Text('¡EMPATE!', style: AppTypography.headlineLg.copyWith(color: AppColors.primary)),
          const SizedBox(height: AppSpacing.xs),
          Text(
            tieBreak.allTiedCase
                ? 'Todos los jugadores quedaron empatados.'
                : 'Hay jugadores empatados por ${tieBreak.affectedLoserSlots == 1 ? 'el último puesto' : '${tieBreak.affectedLoserSlots} puestos'} de perdedor.',
            textAlign: TextAlign.center,
            style: AppTypography.bodyMd.copyWith(color: AppColors.textSecondary),
          ),
          if (tieBreak.tiedPlayerIds.isNotEmpty) ...[
            const SizedBox(height: AppSpacing.md),
            Wrap(
              alignment: WrapAlignment.center,
              spacing: AppSpacing.sm,
              runSpacing: AppSpacing.sm,
              children: [
                for (final id in tieBreak.tiedPlayerIds)
                  StatusPill(
                    label: id == currentUserId ? 'TÚ' : nameOf(id).toUpperCase(),
                    color: AppColors.primary,
                  ),
              ],
            ),
          ],
        ],
      ),
    );
  }

  List<Widget> _pending() {
    // Fase árbitro: los empatados no coincidieron.
    if (tieBreak.hasDecider) {
      if (_iAmArbiter) {
        return [
          _banner(
            Icons.gavel_rounded,
            AppColors.secondary,
            tieBreak.allTiedCase
                ? 'Como host, tu decisión resuelve el empate total.'
                : 'Fuiste elegido como árbitro: tu decisión es la final.',
          ),
          const SizedBox(height: AppSpacing.md),
          ..._options(
            selected: myArbiterVote,
            enabled: myArbiterVote == null && isConnected,
            asArbiter: true,
          ),
          if (myArbiterVote != null) ...[
            const SizedBox(height: AppSpacing.md),
            _waiting('Decisión enviada. Esperando confirmación del servidor…'),
          ],
        ];
      }
      return [
        _banner(
          Icons.gavel_rounded,
          AppColors.primary,
          'Los empatados no coincidieron. Decide ${nameOf(tieBreak.selectedDeciderId!)}.',
        ),
        const SizedBox(height: AppSpacing.md),
        _waiting('Esperando la decisión del árbitro…'),
      ];
    }

    // Fase de votación de los empatados.
    if (myVote != null) {
      return [
        ..._options(selected: myVote, enabled: false, asArbiter: false),
        const SizedBox(height: AppSpacing.md),
        _waiting('Voto registrado. Esperando a los demás empatados…'),
      ];
    }
    if (!canVoteAsTied) {
      return [
        _banner(Icons.visibility_rounded, AppColors.outline, 'Los jugadores empatados están votando.'),
        const SizedBox(height: AppSpacing.md),
        _waiting('Esperando el resultado del desempate…'),
      ];
    }
    return [
      Text('¿Cómo quieres resolver el empate?', style: AppTypography.headlineSm, textAlign: TextAlign.center),
      const SizedBox(height: AppSpacing.md),
      ..._options(selected: null, enabled: isConnected, asArbiter: false),
      if (tieBreak.tiedPlayerIds.isEmpty) ...[
        const SizedBox(height: AppSpacing.md),
        Text(
          'Solo cuentan los votos de los jugadores empatados; el servidor descarta los demás.',
          textAlign: TextAlign.center,
          style: AppTypography.bodySm,
        ),
      ],
    ];
  }

  List<Widget> _options({
    required TieBreakDecision? selected,
    required bool enabled,
    required bool asArbiter,
  }) {
    return [
      for (final decision in TieBreakDecision.values) ...[
        _OptionCard(
          decision: decision,
          selected: selected == decision,
          enabled: enabled,
          onTap: () => onVote(decision, asArbiter: asArbiter),
        ),
        const SizedBox(height: AppSpacing.sm),
      ],
    ];
  }

  Widget _resolved() {
    final mode = tieBreak.resolutionMode;
    final detail = switch (mode) {
      TieBreakDecision.nuevaRonda => 'Los empatados jugarán una ronda de desempate.',
      TieBreakDecision.dividirCuenta => tieBreak.allTiedCase
          ? 'Empate total: cada jugador paga su propia bebida, sin deudas.'
          : 'La cuenta se divide según las reglas económicas.',
      null => 'El servidor resolvió el desempate.',
    };
    return GameCard(
      borderColor: AppColors.secondary.withValues(alpha: 0.6),
      child: Column(
        children: [
          const Icon(Icons.check_circle_rounded, color: AppColors.secondary, size: 36),
          const SizedBox(height: AppSpacing.sm),
          Text(
            mode?.label.toUpperCase() ?? 'RESUELTO',
            style: AppTypography.headlineMd.copyWith(color: AppColors.secondary),
          ),
          const SizedBox(height: AppSpacing.xs),
          Text(detail, textAlign: TextAlign.center, style: AppTypography.bodyMd),
        ],
      ),
    );
  }

  Widget _banner(IconData icon, Color color, String text) => GameCard(
        color: color.withValues(alpha: 0.08),
        borderColor: color.withValues(alpha: 0.4),
        child: Row(
          children: [
            Icon(icon, color: color),
            const SizedBox(width: AppSpacing.md),
            Expanded(child: Text(text, style: AppTypography.labelMd)),
          ],
        ),
      );

  Widget _waiting(String text) => Row(
        mainAxisAlignment: MainAxisAlignment.center,
        children: [
          const SizedBox(
            width: 16,
            height: 16,
            child: CircularProgressIndicator(strokeWidth: 2, color: AppColors.primary),
          ),
          const SizedBox(width: AppSpacing.sm),
          Flexible(child: Text(text, style: AppTypography.bodySm)),
        ],
      );
}

class _OptionCard extends StatelessWidget {
  final TieBreakDecision decision;
  final bool selected;
  final bool enabled;
  final VoidCallback onTap;

  const _OptionCard({
    required this.decision,
    required this.selected,
    required this.enabled,
    required this.onTap,
  });

  @override
  Widget build(BuildContext context) {
    final accent = decision == TieBreakDecision.nuevaRonda ? AppColors.primary : AppColors.secondary;
    final icon = decision == TieBreakDecision.nuevaRonda ? Icons.replay_rounded : Icons.call_split_rounded;
    final dimmed = !enabled && !selected;

    return Opacity(
      opacity: dimmed ? 0.5 : 1,
      child: Material(
        color: selected ? accent.withValues(alpha: 0.14) : AppColors.surfaceContainer,
        shape: RoundedRectangleBorder(
          borderRadius: AppSpacing.borderRadiusMd,
          side: BorderSide(color: selected ? accent : AppColors.surfaceContainerHighest, width: selected ? 2 : 1),
        ),
        child: InkWell(
          borderRadius: AppSpacing.borderRadiusMd,
          onTap: enabled ? onTap : null,
          child: Padding(
            padding: const EdgeInsets.all(AppSpacing.md),
            child: Row(
              children: [
                Container(
                  width: AppSpacing.avatarSize,
                  height: AppSpacing.avatarSize,
                  decoration: BoxDecoration(color: accent.withValues(alpha: 0.15), shape: BoxShape.circle),
                  child: Icon(icon, color: accent),
                ),
                const SizedBox(width: AppSpacing.md),
                Expanded(
                  child: Column(
                    crossAxisAlignment: CrossAxisAlignment.start,
                    children: [
                      Text(decision.label, style: AppTypography.headlineSm),
                      const SizedBox(height: 2),
                      Text(decision.description, style: AppTypography.bodySm),
                    ],
                  ),
                ),
                if (selected) Icon(Icons.check_circle_rounded, color: accent),
              ],
            ),
          ),
        ),
      ),
    );
  }
}
