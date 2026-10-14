import 'package:flutter/material.dart';
import 'package:flutter_riverpod/flutter_riverpod.dart';

import '../../../domain/entities/voice_entities.dart';
import '../../providers/voice_notifier.dart';
import '../../providers/voice_providers.dart';
import '../../theme/colors.dart';
import '../../theme/spacing.dart';
import '../../theme/typography.dart';

/// Barra inferior de voz de la mesa (Sección 27): unirse/salir, micrófono,
/// altavoz y quién está hablando. Solo para jugadores (no espectadores).
class VoiceControlBar extends ConsumerWidget {
  final String gameId;
  final String currentUserId;
  final String Function(String userId) nameOf;

  const VoiceControlBar({
    super.key,
    required this.gameId,
    required this.currentUserId,
    required this.nameOf,
  });

  @override
  Widget build(BuildContext context, WidgetRef ref) {
    final voice = ref.watch(voiceProvider(gameId));
    final notifier = ref.read(voiceProvider(gameId).notifier);

    return Container(
      decoration: const BoxDecoration(
        color: AppColors.surfaceContainerLow,
        border: Border(top: BorderSide(color: AppColors.surfaceContainerHighest)),
      ),
      child: SafeArea(
        top: false,
        child: Padding(
          padding: const EdgeInsets.fromLTRB(AppSpacing.margin, AppSpacing.sm, AppSpacing.margin, AppSpacing.sm),
          child: AnimatedSize(
            duration: const Duration(milliseconds: 200),
            child: voice.isInCall ? _inCall(voice, notifier) : _outOfCall(voice, notifier),
          ),
        ),
      ),
    );
  }

  Widget _outOfCall(VoiceState voice, VoiceNotifier notifier) {
    final connecting = voice.isBusy;
    return Row(
      children: [
        Container(
          width: AppSpacing.iconButtonSize,
          height: AppSpacing.iconButtonSize,
          decoration: BoxDecoration(
            color: AppColors.secondary.withValues(alpha: 0.12),
            shape: BoxShape.circle,
          ),
          child: const Icon(Icons.headset_mic_rounded, color: AppColors.secondary, size: 22),
        ),
        const SizedBox(width: AppSpacing.md),
        Expanded(
          child: Column(
            crossAxisAlignment: CrossAxisAlignment.start,
            mainAxisSize: MainAxisSize.min,
            children: [
              Text('Voz de la mesa', style: AppTypography.labelMd),
              const SizedBox(height: 2),
              Text(
                connecting
                    ? 'Conectando…'
                    : voice.error ?? 'Habla con los jugadores durante la ronda.',
                maxLines: 2,
                overflow: TextOverflow.ellipsis,
                style: AppTypography.bodySm.copyWith(
                  color: voice.error != null && !connecting ? AppColors.error : AppColors.textSecondary,
                ),
              ),
            ],
          ),
        ),
        const SizedBox(width: AppSpacing.sm),
        FilledButton.icon(
          onPressed: connecting ? null : notifier.join,
          icon: connecting
              ? const SizedBox(
                  width: 16,
                  height: 16,
                  child: CircularProgressIndicator(strokeWidth: 2, color: AppColors.onSecondary),
                )
              : const Icon(Icons.call_rounded, size: 18),
          label: Text(voice.error != null ? 'Reintentar' : 'Unirse'),
          style: FilledButton.styleFrom(
            backgroundColor: AppColors.secondary,
            foregroundColor: AppColors.onSecondary,
            disabledBackgroundColor: AppColors.secondary.withValues(alpha: 0.5),
            minimumSize: const Size(0, AppSpacing.iconButtonSize),
            shape: RoundedRectangleBorder(borderRadius: AppSpacing.borderRadiusFull),
            textStyle: AppTypography.labelMd,
          ),
        ),
      ],
    );
  }

  Widget _inCall(VoiceState voice, VoiceNotifier notifier) {
    final reconnecting = voice.status == VoiceConnectionStatus.reconnecting;
    return Column(
      mainAxisSize: MainAxisSize.min,
      children: [
        if (reconnecting || voice.error != null)
          Padding(
            padding: const EdgeInsets.only(bottom: AppSpacing.xs),
            child: Row(
              children: [
                if (reconnecting) ...[
                  const SizedBox(
                    width: 12,
                    height: 12,
                    child: CircularProgressIndicator(strokeWidth: 1.5, color: AppColors.primary),
                  ),
                  const SizedBox(width: AppSpacing.sm),
                ],
                Expanded(
                  child: Text(
                    reconnecting ? 'Reconectando la voz…' : voice.error!,
                    style: AppTypography.bodySm.copyWith(
                      color: reconnecting ? AppColors.primary : AppColors.error,
                    ),
                  ),
                ),
              ],
            ),
          ),
        Row(
          children: [
            Expanded(child: _participants(voice)),
            _RoundIconButton(
              icon: voice.speakerOn ? Icons.volume_up_rounded : Icons.phone_in_talk_rounded,
              tooltip: voice.speakerOn ? 'Usar auricular' : 'Usar altavoz',
              onPressed: notifier.toggleSpeaker,
            ),
            const SizedBox(width: AppSpacing.sm),
            _MicButton(
              enabled: voice.micEnabled,
              busy: voice.isTogglingMic,
              onPressed: reconnecting ? null : notifier.toggleMic,
            ),
            const SizedBox(width: AppSpacing.sm),
            _RoundIconButton(
              icon: Icons.call_end_rounded,
              tooltip: 'Salir de la voz',
              color: AppColors.error,
              background: AppColors.errorContainer.withValues(alpha: 0.5),
              onPressed: notifier.leave,
            ),
          ],
        ),
      ],
    );
  }

  Widget _participants(VoiceState voice) {
    final people = voice.participants;
    final speaking = people.where((p) => p.isSpeaking).toList();
    final label = speaking.isNotEmpty
        ? 'Hablando: ${speaking.map((p) => p.isLocal ? 'Tú' : nameOf(p.identity)).join(', ')}'
        : '${people.length} en la voz';
    return Row(
      children: [
        SizedBox(
          height: 32,
          width: 32.0 + (people.length.clamp(1, 4) - 1) * 20,
          child: Stack(
            children: [
              for (var i = 0; i < people.length && i < 4; i++)
                Positioned(left: i * 20.0, child: _avatar(people[i])),
            ],
          ),
        ),
        const SizedBox(width: AppSpacing.sm),
        Expanded(
          child: Text(
            label,
            maxLines: 1,
            overflow: TextOverflow.ellipsis,
            style: AppTypography.bodySm.copyWith(
              color: speaking.isNotEmpty ? AppColors.secondary : AppColors.textSecondary,
            ),
          ),
        ),
      ],
    );
  }

  Widget _avatar(VoiceParticipant p) {
    final name = p.isLocal ? 'Tú' : nameOf(p.identity);
    return Container(
      width: 32,
      height: 32,
      alignment: Alignment.center,
      decoration: BoxDecoration(
        shape: BoxShape.circle,
        color: AppColors.surfaceContainerHigh,
        border: Border.all(
          color: p.isSpeaking ? AppColors.secondary : AppColors.surfaceContainerLow,
          width: p.isSpeaking ? 2.5 : 2,
        ),
      ),
      child: p.isMuted && !p.isSpeaking
          ? const Icon(Icons.mic_off_rounded, size: 14, color: AppColors.outline)
          : Text(name.characters.first.toUpperCase(), style: AppTypography.labelSm.copyWith(color: AppColors.textPrimary)),
    );
  }
}

class _MicButton extends StatelessWidget {
  final bool enabled;
  final bool busy;
  final VoidCallback? onPressed;

  const _MicButton({required this.enabled, required this.busy, required this.onPressed});

  @override
  Widget build(BuildContext context) {
    final bg = enabled ? AppColors.secondary : AppColors.surfaceContainerHighest;
    final fg = enabled ? AppColors.onSecondary : AppColors.textPrimary;
    return Semantics(
      button: true,
      toggled: enabled,
      label: enabled ? 'Silenciar micrófono' : 'Activar micrófono',
      child: Material(
        color: bg,
        shape: const CircleBorder(),
        child: InkWell(
          customBorder: const CircleBorder(),
          onTap: busy ? null : onPressed,
          child: SizedBox(
            width: AppSpacing.buttonPrimaryHeight,
            height: AppSpacing.buttonPrimaryHeight,
            child: Center(
              child: busy
                  ? SizedBox(width: 20, height: 20, child: CircularProgressIndicator(strokeWidth: 2, color: fg))
                  : Icon(enabled ? Icons.mic_rounded : Icons.mic_off_rounded, color: fg, size: 26),
            ),
          ),
        ),
      ),
    );
  }
}

class _RoundIconButton extends StatelessWidget {
  final IconData icon;
  final String tooltip;
  final VoidCallback? onPressed;
  final Color color;
  final Color background;

  const _RoundIconButton({
    required this.icon,
    required this.tooltip,
    required this.onPressed,
    this.color = AppColors.textPrimary,
    this.background = AppColors.surfaceContainerHigh,
  });

  @override
  Widget build(BuildContext context) {
    return IconButton.filled(
      onPressed: onPressed,
      tooltip: tooltip,
      icon: Icon(icon, size: 22),
      style: IconButton.styleFrom(
        backgroundColor: background,
        foregroundColor: color,
        minimumSize: const Size.square(AppSpacing.iconButtonSize),
      ),
    );
  }
}
