import 'package:flutter/material.dart';

import '../../../domain/entities/game_decisions.dart';
import '../../../domain/entities/reveal_attempt_entity.dart';
import '../../theme/colors.dart';
import '../../theme/spacing.dart';
import '../../theme/typography.dart';
import 'game_card.dart';
import 'reveal_photo.dart';

/// Tarjeta de un RevealAttempt con panel de votación paralela SÍ / NO
/// (Secciones 22-23). La elegibilidad solo habilita la UI; el backend valida.
class AttemptVoteCard extends StatelessWidget {
  final RevealAttemptEntity attempt;
  final String revealerName;
  final bool isOwn;
  final bool canVote;
  final bool votingSupported;
  final String? ineligibleReason;
  final RevealVoteDecision? myVote;
  final ValueChanged<RevealVoteDecision> onVote;

  const AttemptVoteCard({
    super.key,
    required this.attempt,
    required this.revealerName,
    required this.isOwn,
    required this.canVote,
    required this.votingSupported,
    required this.onVote,
    this.ineligibleReason,
    this.myVote,
  });

  @override
  Widget build(BuildContext context) {
    final (statusLabel, statusColor) = switch (attempt.status) {
      AttemptStatus.pending => ('EN VOTACIÓN', AppColors.primary),
      AttemptStatus.approved => ('APROBADO', AppColors.secondary),
      AttemptStatus.rejected => ('RECHAZADO', AppColors.error),
    };

    return GameCard(
      borderColor: isOwn ? AppColors.primary.withValues(alpha: 0.5) : null,
      child: Column(
        crossAxisAlignment: CrossAxisAlignment.stretch,
        children: [
          Row(
            children: [
              Expanded(
                child: Column(
                  crossAxisAlignment: CrossAxisAlignment.start,
                  children: [
                    Text(isOwn ? 'Tu intento' : revealerName, style: AppTypography.headlineSm),
                    const SizedBox(height: 2),
                    Text(
                      'Intento ${attempt.attemptNumber} de ${RevealAttemptEntity.maxAttempts}',
                      style: AppTypography.bodySm,
                    ),
                  ],
                ),
              ),
              StatusPill(label: statusLabel, color: statusColor),
            ],
          ),
          const SizedBox(height: AppSpacing.md),
          Stack(
            children: [
              RevealPhoto(fileKey: attempt.photoReference, height: 180),
              Positioned(
                right: AppSpacing.sm,
                bottom: AppSpacing.sm,
                child: CapNumberBadge(number: attempt.proposedNumber, size: 72),
              ),
            ],
          ),
          const SizedBox(height: AppSpacing.md),
          _buildVoteArea(),
        ],
      ),
    );
  }

  Widget _buildVoteArea() {
    if (isOwn) {
      return _hint(Icons.hourglass_top_rounded, 'Los demás jugadores están votando tu número.');
    }
    if (myVote != null) {
      final color = myVote == RevealVoteDecision.yes ? AppColors.secondary : AppColors.error;
      return Row(
        mainAxisAlignment: MainAxisAlignment.center,
        children: [
          Icon(Icons.how_to_vote_rounded, color: color, size: 18),
          const SizedBox(width: AppSpacing.sm),
          Text('Votaste ${myVote!.label}', style: AppTypography.labelMd.copyWith(color: color)),
        ],
      );
    }

    final enabled = canVote && votingSupported;
    return Column(
      crossAxisAlignment: CrossAxisAlignment.stretch,
      children: [
        Text(
          '¿El número de la foto es ${attempt.proposedNumber}?',
          textAlign: TextAlign.center,
          style: AppTypography.labelMd,
        ),
        const SizedBox(height: AppSpacing.sm),
        Row(
          children: [
            Expanded(
              child: _VoteButton(
                label: 'NO',
                icon: Icons.close_rounded,
                background: AppColors.errorContainer,
                foreground: AppColors.error,
                onPressed: enabled ? () => onVote(RevealVoteDecision.no) : null,
              ),
            ),
            const SizedBox(width: AppSpacing.gutter),
            Expanded(
              child: _VoteButton(
                label: 'SÍ',
                icon: Icons.check_rounded,
                background: AppColors.secondary,
                foreground: AppColors.onSecondary,
                onPressed: enabled ? () => onVote(RevealVoteDecision.yes) : null,
              ),
            ),
          ],
        ),
        if (!canVote && ineligibleReason != null) ...[
          const SizedBox(height: AppSpacing.sm),
          _hint(Icons.info_outline_rounded, ineligibleReason!),
        ] else if (!votingSupported) ...[
          const SizedBox(height: AppSpacing.sm),
          _hint(
            Icons.construction_rounded,
            'La votación de revelaciones aún no está habilitada en el servidor.',
          ),
        ],
      ],
    );
  }

  Widget _hint(IconData icon, String text) => Row(
        crossAxisAlignment: CrossAxisAlignment.start,
        children: [
          Icon(icon, size: 16, color: AppColors.outline),
          const SizedBox(width: AppSpacing.sm),
          Expanded(child: Text(text, style: AppTypography.bodySm)),
        ],
      );
}

class _VoteButton extends StatelessWidget {
  final String label;
  final IconData icon;
  final Color background;
  final Color foreground;
  final VoidCallback? onPressed;

  const _VoteButton({
    required this.label,
    required this.icon,
    required this.background,
    required this.foreground,
    required this.onPressed,
  });

  @override
  Widget build(BuildContext context) {
    return ElevatedButton.icon(
      onPressed: onPressed,
      icon: Icon(icon, size: 22),
      label: Text(label, style: AppTypography.labelLg.copyWith(color: foreground)),
      style: ElevatedButton.styleFrom(
        backgroundColor: background,
        foregroundColor: foreground,
        disabledBackgroundColor: AppColors.surfaceContainerHigh,
        disabledForegroundColor: AppColors.outline,
        minimumSize: const Size.fromHeight(AppSpacing.buttonPrimaryHeight),
        shape: RoundedRectangleBorder(borderRadius: AppSpacing.borderRadiusMd),
        elevation: 0,
      ),
    );
  }
}
