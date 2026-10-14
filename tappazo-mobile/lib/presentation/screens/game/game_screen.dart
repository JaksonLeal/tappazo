import 'package:flutter/material.dart';
import 'package:flutter_riverpod/flutter_riverpod.dart';

import '../../../core/utils/currency_formatter.dart';
import '../../../domain/entities/game_details_entity.dart';
import '../../../domain/entities/game_mode.dart';
import '../../../domain/entities/game_participant_entity.dart';
import '../../../domain/entities/reveal_attempt_entity.dart';
import '../../../domain/entities/round_entity.dart';
import '../../providers/game_providers.dart';
import '../../providers/game_session_notifier.dart';
import '../../providers/game_session_providers.dart';
import '../../providers/game_session_state.dart';
import '../../providers/game_state.dart';
import '../../theme/colors.dart';
import '../../theme/spacing.dart';
import '../../theme/typography.dart';
import '../../widgets/game/attempt_vote_card.dart';
import '../../widgets/game/connection_badge.dart';
import '../../widgets/game/game_card.dart';
import '../../widgets/game/round_resolution_panel.dart';
import '../../widgets/game/tie_break_panel.dart';
import '../../widgets/tappazo_button.dart';
import '../mesa/mesa_screen.dart';
import 'reveal_capture_screen.dart';

/// Pantalla de ronda en curso (Game.state == IN_PROGRESS).
/// Todo lo que muestra proviene de eventos STOMP o del REST del backend.
class GameScreen extends ConsumerWidget {
  final String gameId;

  const GameScreen({super.key, required this.gameId});

  @override
  Widget build(BuildContext context, WidgetRef ref) {
    final provider = gameSessionProvider(gameId);
    final session = ref.watch(provider);
    final notifier = ref.read(provider.notifier);
    final gameState = ref.watch(gameNotifierProvider);
    final details = gameState is GameLoaded && gameState.game.id == gameId ? gameState.details : null;
    final userId = ref.watch(currentUserIdProvider);

    ref.listen<GameNotice?>(provider.select((s) => s.notice), (prev, next) {
      if (next == null || next.id == prev?.id) return;
      ScaffoldMessenger.of(context)
        ..hideCurrentSnackBar()
        ..showSnackBar(SnackBar(
          content: Text(next.message),
          behavior: SnackBarBehavior.floating,
          backgroundColor: next.isError ? AppColors.errorContainer : AppColors.surfaceContainerHighest,
        ));
    });

    final view = _GameView(
      gameId: gameId,
      session: session,
      notifier: notifier,
      details: details,
      userId: userId,
    );

    return Scaffold(
      backgroundColor: AppColors.background,
      appBar: AppBar(
        title: Text(_title(session), style: AppTypography.headlineSm),
        actions: [
          Center(child: ConnectionBadge(status: session.connection, onRetry: notifier.reconnect)),
          IconButton(
            icon: const Icon(Icons.table_restaurant_rounded, color: AppColors.primary),
            tooltip: 'Ver Mesa y Deudas',
            onPressed: () => Navigator.of(context).push(
              MaterialPageRoute(builder: (_) => MesaScreen(gameId: gameId)),
            ),
          ),
        ],
      ),
      body: RefreshIndicator(
        color: AppColors.primary,
        backgroundColor: AppColors.surfaceContainer,
        onRefresh: notifier.resync,
        child: AnimatedSwitcher(
          duration: const Duration(milliseconds: 250),
          child: ListView(
            key: ValueKey(session.phase),
            physics: const AlwaysScrollableScrollPhysics(),
            padding: const EdgeInsets.symmetric(horizontal: AppSpacing.margin, vertical: AppSpacing.md),
            children: view.build(context),
          ),
        ),
      ),
    );
  }

  String _title(GameSessionState s) => switch (s.phase) {
        GamePhase.waitingForRound => 'ESPERANDO RONDA',
        GamePhase.revealing => s.round?.isTieBreakRound == true ? 'RONDA DE DESEMPATE' : 'RONDA EN CURSO',
        GamePhase.tieBreak => 'DESEMPATE',
        GamePhase.resolved => 'RESULTADO',
        GamePhase.finished => 'PARTIDA TERMINADA',
      };
}

/// Construye el contenido por fase. Separado para mantener legible el build.
class _GameView {
  final String gameId;
  final GameSessionState session;
  final GameSessionNotifier notifier;
  final GameDetailsEntity? details;
  final String userId;

  _GameView({
    required this.gameId,
    required this.session,
    required this.notifier,
    required this.details,
    required this.userId,
  });

  GameParticipantEntity? get _me => details?.participantFor(userId);

  String _nameOf(String id) => details?.displayNameFor(id) ?? 'Jugador ${id.substring(0, id.length > 5 ? 5 : id.length)}';

  List<Widget> build(BuildContext context) {
    final round = session.round;
    return switch (session.phase) {
      GamePhase.waitingForRound => _waiting(context),
      GamePhase.revealing => [
          if (round != null) _roundHeader(round),
          const SizedBox(height: AppSpacing.md),
          _myRevealCard(context, round!),
          const SizedBox(height: AppSpacing.lg),
          _playersStrip(round),
          const SizedBox(height: AppSpacing.lg),
          ..._attemptsFeed(round),
        ],
      GamePhase.tieBreak => _tieBreak(),
      GamePhase.resolved => _resolved(context),
      GamePhase.finished => _finished(context),
    };
  }

  // ─── Encabezado de la ronda ───────────────────────────────────────────────

  Widget _roundHeader(RoundEntity round) {
    return GameCard(
      child: Row(
        children: [
          Expanded(
            child: Column(
              crossAxisAlignment: CrossAxisAlignment.start,
              children: [
                Text(GameMode.fromString(round.mode).displayName, style: AppTypography.headlineMd),
                const SizedBox(height: 2),
                Text('Bebida: ${CurrencyFormatter.format(round.drinkPrice)}', style: AppTypography.bodySm),
              ],
            ),
          ),
          Column(
            crossAxisAlignment: CrossAxisAlignment.end,
            children: [
              StatusPill(
                label: round.state.displayName.toUpperCase(),
                color: round.state == RoundState.validating ? AppColors.secondary : AppColors.primary,
              ),
              if (round.isTieBreakRound) ...[
                const SizedBox(height: AppSpacing.xs),
                const StatusPill(label: 'DESEMPATE', color: AppColors.primary, icon: Icons.balance_rounded),
              ],
            ],
          ),
        ],
      ),
    );
  }

  // ─── Turno de revelación propio ───────────────────────────────────────────

  Widget _myRevealCard(BuildContext context, RoundEntity round) {
    final me = _me;
    if (me == null) {
      return _infoCard(Icons.person_off_outlined, 'No participas en esta ronda.');
    }
    if (me.isSpectator) {
      return _infoCard(Icons.visibility_rounded, 'Eres espectador: observa la ronda y entrarás como jugador en la próxima.');
    }
    if (!me.isActive) {
      return _infoCard(Icons.pause_circle_outline_rounded, 'Estás en stand-by: no participas en esta ronda.');
    }

    final sub = session.submission;
    if (sub.isInFlight) {
      final text = switch (sub.stage) {
        SubmissionStage.uploading => 'Subiendo la foto de tu tapa…',
        SubmissionStage.sending => 'Enviando tu intento…',
        _ => 'Esperando confirmación del servidor…',
      };
      return GameCard(
        borderColor: AppColors.primary.withValues(alpha: 0.5),
        child: Row(
          children: [
            const SizedBox(
              width: 22,
              height: 22,
              child: CircularProgressIndicator(strokeWidth: 2.5, color: AppColors.primary),
            ),
            const SizedBox(width: AppSpacing.md),
            Expanded(child: Text(text, style: AppTypography.labelMd)),
          ],
        ),
      );
    }
    if (sub.stage == SubmissionStage.unconfirmed || sub.stage == SubmissionStage.failed) {
      return GameCard(
        color: AppColors.errorContainer.withValues(alpha: 0.2),
        borderColor: AppColors.error.withValues(alpha: 0.5),
        child: Column(
          crossAxisAlignment: CrossAxisAlignment.stretch,
          children: [
            Row(
              crossAxisAlignment: CrossAxisAlignment.start,
              children: [
                const Icon(Icons.error_outline_rounded, color: AppColors.error),
                const SizedBox(width: AppSpacing.sm),
                Expanded(child: Text(sub.error ?? 'No se pudo confirmar tu intento.', style: AppTypography.bodyMd)),
              ],
            ),
            const SizedBox(height: AppSpacing.md),
            Row(
              children: [
                Expanded(
                  child: TappazoButton(
                    text: 'Cerrar',
                    isOutlined: true,
                    onPressed: notifier.dismissSubmissionError,
                  ),
                ),
                const SizedBox(width: AppSpacing.gutter),
                Expanded(
                  child: TappazoButton(
                    text: 'Reintentar',
                    icon: Icons.photo_camera_rounded,
                    onPressed: () {
                      notifier.dismissSubmissionError();
                      _openCapture(context, null);
                    },
                  ),
                ),
              ],
            ),
          ],
        ),
      );
    }

    // Estado del último intento propio (si existe).
    final own = session.latestAttemptsPerReveal.where((a) => session.isOwnAttempt(a, userId)).toList();
    final latestOwn = own.isEmpty ? null : own.first;
    if (latestOwn != null) {
      switch (latestOwn.status) {
        case AttemptStatus.pending:
          return _infoCard(
            Icons.how_to_vote_rounded,
            'Tu ${latestOwn.proposedNumber.toString().padLeft(2, '0')} está en votación.',
            color: AppColors.primary,
          );
        case AttemptStatus.approved:
          return _infoCard(
            Icons.verified_rounded,
            'Tu número ${latestOwn.proposedNumber.toString().padLeft(2, '0')} quedó oficial.',
            color: AppColors.secondary,
          );
        case AttemptStatus.rejected:
          if (latestOwn.attemptNumber >= RevealAttemptEntity.maxAttempts) {
            return _infoCard(
              Icons.gavel_rounded,
              'Tercer rechazo: el árbitro revisará tu foto y número.',
              color: AppColors.error,
            );
          }
          break;
      }
    }

    if (!round.state.acceptsReveals) {
      return _infoCard(Icons.hourglass_empty_rounded, 'La ronda aún no acepta revelaciones.');
    }

    final turnKnown = round.hasTurnInfo;
    final isMyTurn = !turnKnown || round.currentTurnPlayerId == userId;
    if (!isMyTurn) {
      return _infoCard(
        Icons.hourglass_top_rounded,
        'Turno de ${_nameOf(round.currentTurnPlayerId!)}. Espera tu turno para revelar.',
      );
    }

    final nextAttempt = latestOwn != null ? latestOwn.attemptNumber + 1 : null;
    return GameCard(
      borderColor: AppColors.primary.withValues(alpha: 0.6),
      child: Column(
        crossAxisAlignment: CrossAxisAlignment.stretch,
        children: [
          Text(
            turnKnown ? '¡ES TU TURNO!' : 'REVELA TU TAPA',
            textAlign: TextAlign.center,
            style: AppTypography.headlineLg.copyWith(color: AppColors.primary),
          ),
          const SizedBox(height: AppSpacing.xs),
          Text(
            nextAttempt != null
                ? 'Tu intento anterior fue rechazado. Toma una foto nueva (intento $nextAttempt de ${RevealAttemptEntity.maxAttempts}).'
                : turnKnown
                    ? 'Fotografía tu tapa y confirma el número.'
                    : 'Cuando te toque según el orden de la mesa, fotografía tu tapa y confirma el número.',
            textAlign: TextAlign.center,
            style: AppTypography.bodyMd.copyWith(color: AppColors.textSecondary),
          ),
          const SizedBox(height: AppSpacing.md),
          TappazoButton(
            text: 'Revelar mi tapa',
            icon: Icons.photo_camera_rounded,
            onPressed: session.isConnected ? () => _openCapture(context, nextAttempt) : null,
          ),
          if (!session.isConnected) ...[
            const SizedBox(height: AppSpacing.sm),
            Text(
              'Esperando conexión en tiempo real…',
              textAlign: TextAlign.center,
              style: AppTypography.bodySm,
            ),
          ],
        ],
      ),
    );
  }

  void _openCapture(BuildContext context, int? nextAttempt) {
    Navigator.of(context).push(
      MaterialPageRoute(
        fullscreenDialog: true,
        builder: (_) => RevealCaptureScreen(gameId: gameId, nextAttemptNumber: nextAttempt),
      ),
    );
  }

  // ─── Jugadores / orden de revelación ──────────────────────────────────────

  Widget _playersStrip(RoundEntity round) {
    final hasOrder = round.turnOrder.isNotEmpty;
    final ids = hasOrder
        ? round.turnOrder
        : (details?.participantsSortedAlphabetically.where((p) => p.isPlayer).map((p) => p.userId).toList() ??
            const <String>[]);
    if (ids.isEmpty) return const SizedBox.shrink();

    return Column(
      crossAxisAlignment: CrossAxisAlignment.start,
      children: [
        Text(hasOrder ? 'ORDEN DE REVELACIÓN' : 'JUGADORES', style: AppTypography.labelSm),
        const SizedBox(height: AppSpacing.sm),
        SizedBox(
          height: 84,
          child: ListView.separated(
            scrollDirection: Axis.horizontal,
            itemCount: ids.length,
            separatorBuilder: (_, __) => const SizedBox(width: AppSpacing.md),
            itemBuilder: (_, i) {
              final id = ids[i];
              final participant = details?.participantFor(id);
              final isTurn = round.currentTurnPlayerId == id;
              final isMe = id == userId;
              final inactive = participant != null && !participant.isActive;
              final ringColor = isTurn ? AppColors.primary : (isMe ? AppColors.secondary : AppColors.surfaceContainerHighest);
              final name = isMe ? 'Tú' : _nameOf(id);
              return Opacity(
                opacity: inactive ? 0.45 : 1,
                child: SizedBox(
                  width: 64,
                  child: Column(
                    children: [
                      Container(
                        width: AppSpacing.avatarSize,
                        height: AppSpacing.avatarSize,
                        alignment: Alignment.center,
                        decoration: BoxDecoration(
                          shape: BoxShape.circle,
                          color: AppColors.surfaceContainerHigh,
                          border: Border.all(color: ringColor, width: isTurn ? 3 : 2),
                        ),
                        child: Text(
                          hasOrder ? '${i + 1}' : name.characters.first.toUpperCase(),
                          style: AppTypography.labelLg,
                        ),
                      ),
                      const SizedBox(height: AppSpacing.xs),
                      Text(
                        name,
                        maxLines: 1,
                        overflow: TextOverflow.ellipsis,
                        style: AppTypography.bodySm.copyWith(
                          color: isTurn ? AppColors.primary : AppColors.textSecondary,
                        ),
                      ),
                    ],
                  ),
                ),
              );
            },
          ),
        ),
      ],
    );
  }

  // ─── Revelaciones y votación paralela ─────────────────────────────────────

  List<Widget> _attemptsFeed(RoundEntity round) {
    final attempts = session.latestAttemptsPerReveal;
    if (attempts.isEmpty) {
      return [
        Text('REVELACIONES', style: AppTypography.labelSm),
        const SizedBox(height: AppSpacing.sm),
        _infoCard(Icons.casino_outlined, 'Aún nadie ha revelado su tapa.'),
      ];
    }

    return [
      Text('REVELACIONES', style: AppTypography.labelSm),
      const SizedBox(height: AppSpacing.sm),
      for (final attempt in attempts) ...[
        _attemptCard(round, attempt),
        const SizedBox(height: AppSpacing.md),
      ],
    ];
  }

  Widget _attemptCard(RoundEntity round, RevealAttemptEntity attempt) {
    final isOwn = session.isOwnAttempt(attempt, userId);
    final reason = _voteIneligibility(round, attempt);
    return AttemptVoteCard(
      key: ValueKey(attempt.id),
      attempt: attempt,
      revealerName: attempt.playerId != null ? _nameOf(attempt.playerId!) : 'Revelación de un jugador',
      isOwn: isOwn,
      canVote: !isOwn && reason == null,
      ineligibleReason: reason,
      votingSupported: notifier.isRevealVotingSupported,
      myVote: session.myVotes[attempt.id],
      onVote: (decision) => notifier.castRevealVote(attempt, decision),
    );
  }

  /// Votantes elegibles (Sección 22): ACTIVE, no espectador, no revelador.
  /// Solo controla la UI; la validez del voto la determina el backend.
  String? _voteIneligibility(RoundEntity round, RevealAttemptEntity attempt) {
    final me = _me;
    if (me == null || me.isSpectator) return 'Los espectadores no votan.';
    if (!me.isActive) return 'Estás en stand-by: no votas en esta ronda.';
    if (!attempt.isPending) return 'Este intento ya fue resuelto.';
    if (!round.state.acceptsReveals) return 'La votación de esta ronda está cerrada.';
    return null;
  }

  // ─── Desempate ────────────────────────────────────────────────────────────

  List<Widget> _tieBreak() {
    final tieBreak = session.tieBreak;
    if (tieBreak == null) {
      return [_infoCard(Icons.balance_rounded, 'Desempate en curso. Sincronizando con el servidor…', color: AppColors.primary)];
    }
    final me = _me;
    final canVoteAsTied = tieBreak.tiedPlayerIds.isNotEmpty
        ? tieBreak.tiedPlayerIds.contains(userId)
        : (me != null && me.isPlayer);
    return [
      TieBreakPanel(
        tieBreak: tieBreak,
        currentUserId: userId,
        nameOf: _nameOf,
        canVoteAsTied: canVoteAsTied,
        myVote: session.myTieBreakVote,
        myArbiterVote: session.myArbiterVote,
        isConnected: session.isConnected,
        onVote: (decision, {required bool asArbiter}) =>
            notifier.castTieBreakVote(decision, asArbiter: asArbiter),
      ),
    ];
  }

  // ─── Resultado ────────────────────────────────────────────────────────────

  List<Widget> _resolved(BuildContext context) {
    final resolution = session.resolution;
    final tieBreak = session.tieBreak;
    return [
      if (tieBreak != null && tieBreak.isResolved) ...[
        TieBreakPanel(
          tieBreak: tieBreak,
          currentUserId: userId,
          nameOf: _nameOf,
          canVoteAsTied: false,
          myVote: session.myTieBreakVote,
          myArbiterVote: session.myArbiterVote,
          isConnected: session.isConnected,
          onVote: (_, {required bool asArbiter}) {},
        ),
        const SizedBox(height: AppSpacing.lg),
      ],
      if (resolution != null)
        RoundResolutionPanel(resolution: resolution, currentUserId: userId, nameOf: _nameOf),
      const SizedBox(height: AppSpacing.xl),
      TappazoButton(
        text: 'Ver Mesa y Deudas',
        icon: Icons.receipt_long_rounded,
        isOutlined: true,
        onPressed: () => Navigator.of(context).push(
          MaterialPageRoute(builder: (_) => MesaScreen(gameId: gameId)),
        ),
      ),
      const SizedBox(height: AppSpacing.md),
      TappazoButton(
        text: 'Volver a la sala',
        icon: Icons.meeting_room_rounded,
        onPressed: () => Navigator.of(context).maybePop(),
      ),
    ];
  }

  List<Widget> _waiting(BuildContext context) => [
        const SizedBox(height: AppSpacing.xl),
        _infoCard(
          Icons.hourglass_empty_rounded,
          'Esperando a que el host inicie la ronda. Recibirás el aviso en tiempo real.',
          color: AppColors.primary,
        ),
        const SizedBox(height: AppSpacing.lg),
        TappazoButton(
          text: 'Volver a la sala',
          isOutlined: true,
          onPressed: () => Navigator.of(context).maybePop(),
        ),
      ];

  List<Widget> _finished(BuildContext context) => [
        const SizedBox(height: AppSpacing.xl),
        _infoCard(Icons.flag_rounded, 'La partida terminó. Revisa la mesa para ver los saldos finales.',
            color: AppColors.secondary),
        const SizedBox(height: AppSpacing.lg),
        TappazoButton(
          text: 'Ver Mesa y Deudas',
          icon: Icons.receipt_long_rounded,
          onPressed: () => Navigator.of(context).push(
            MaterialPageRoute(builder: (_) => MesaScreen(gameId: gameId)),
          ),
        ),
      ];

  Widget _infoCard(IconData icon, String text, {Color color = AppColors.outline}) {
    return GameCard(
      child: Row(
        children: [
          Icon(icon, color: color),
          const SizedBox(width: AppSpacing.md),
          Expanded(child: Text(text, style: AppTypography.bodyMd)),
        ],
      ),
    );
  }
}
