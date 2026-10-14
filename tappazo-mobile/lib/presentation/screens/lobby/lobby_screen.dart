import 'package:flutter/material.dart';
import 'package:flutter/services.dart';
import 'package:flutter_riverpod/flutter_riverpod.dart';
import '../../../core/utils/currency_formatter.dart';
import '../../../domain/entities/game_entity.dart';
import '../../../domain/entities/game_mode.dart';
import '../../providers/auth_providers.dart';
import '../../providers/auth_state.dart';
import '../../providers/game_providers.dart';
import '../../providers/game_session_providers.dart';
import '../../providers/game_state.dart';
import '../../theme/colors.dart';
import '../../theme/spacing.dart';
import '../../theme/typography.dart';
import '../../widgets/game/connection_badge.dart';
import '../../widgets/tappazo_button.dart';
import '../game/game_screen.dart';
import '../mesa/mesa_screen.dart';

class LobbyScreen extends ConsumerStatefulWidget {
  final String gameId;

  const LobbyScreen({super.key, required this.gameId});

  @override
  ConsumerState<LobbyScreen> createState() => _LobbyScreenState();
}

class _LobbyScreenState extends ConsumerState<LobbyScreen> {
  final TextEditingController _drinkPriceController = TextEditingController();
  GameMode? _selectedMode;
  bool _isEditingSettings = false;

  @override
  void initState() {
    super.initState();
    WidgetsBinding.instance.addPostFrameCallback((_) {
      ref.read(gameNotifierProvider.notifier).refreshGameDetails(widget.gameId);
    });
  }

  @override
  void dispose() {
    _drinkPriceController.dispose();
    super.dispose();
  }

  bool _gameScreenOpen = false;

  Future<void> _openGameScreen() async {
    if (_gameScreenOpen || !mounted) return;
    _gameScreenOpen = true;
    await Navigator.of(context).push(
      MaterialPageRoute(builder: (_) => GameScreen(gameId: widget.gameId)),
    );
    _gameScreenOpen = false;
    if (mounted) {
      ref.read(gameNotifierProvider.notifier).refreshGameDetails(widget.gameId);
    }
  }

  @override
  Widget build(BuildContext context) {
    final gameState = ref.watch(gameNotifierProvider);
    final authState = ref.watch(authNotifierProvider);
    final currentUserId = authState is Authenticated ? authState.user.id : '';

    // Mantiene viva la conexión STOMP mientras se está en la sala (Sección 68).
    final sessionProvider = gameSessionProvider(widget.gameId);
    final session = ref.watch(sessionProvider);

    // Cuando el backend inicia una ronda, se abre la pantalla de juego.
    ref.listen<String?>(sessionProvider.select((s) => s.round?.isFinished == false ? s.round?.id : null),
        (previous, next) {
      if (next != null && next != previous) _openGameScreen();
    });

    return Scaffold(
      backgroundColor: AppColors.background,
      appBar: AppBar(
        title: Text('SALA DE ESPERA', style: AppTypography.headlineSm),
        actions: [
          Center(
            child: ConnectionBadge(
              status: session.connection,
              onRetry: ref.read(sessionProvider.notifier).reconnect,
            ),
          ),
          IconButton(
            icon: const Icon(Icons.refresh_rounded),
            tooltip: 'Actualizar sala',
            onPressed: () {
              ref.read(gameNotifierProvider.notifier).refreshGameDetails(widget.gameId);
            },
          ),
          IconButton(
            icon: const Icon(Icons.table_restaurant_rounded, color: AppColors.primary),
            tooltip: 'Ver Mesa y Deudas',
            onPressed: () {
              Navigator.of(context).push(
                MaterialPageRoute(builder: (_) => MesaScreen(gameId: widget.gameId)),
              );
            },
          ),
        ],
      ),
      body: switch (gameState) {
        GameLoading(:final message) => Center(
            child: Column(
              mainAxisAlignment: MainAxisAlignment.center,
              children: [
                const CircularProgressIndicator(
                  strokeWidth: 2.5,
                  valueColor: AlwaysStoppedAnimation<Color>(AppColors.primary),
                ),
                if (message != null) ...[
                  const SizedBox(height: AppSpacing.md),
                  Text(message, style: AppTypography.bodyMd),
                ],
              ],
            ),
          ),
        GameError(:final message) => Center(
            child: Padding(
              padding: const EdgeInsets.all(AppSpacing.margin),
              child: Column(
                mainAxisAlignment: MainAxisAlignment.center,
                children: [
                  const Icon(Icons.error_outline_rounded, size: 48, color: AppColors.error),
                  const SizedBox(height: AppSpacing.md),
                  Text(
                    message,
                    textAlign: TextAlign.center,
                    style: AppTypography.bodyMd.copyWith(color: AppColors.error),
                  ),
                  const SizedBox(height: AppSpacing.lg),
                  TappazoButton(
                    text: 'Reintentar',
                    onPressed: () {
                      ref.read(gameNotifierProvider.notifier).refreshGameDetails(widget.gameId);
                    },
                  ),
                ],
              ),
            ),
          ),
        GameLoaded(:final details, :final isUpdatingSettings) => _buildLobbyContent(
            context,
            details.game,
            details.participantsSortedAlphabetically,
            currentUserId,
            isUpdatingSettings,
          ),
        _ => const SizedBox.shrink(),
      },
    );
  }

  Widget _buildLobbyContent(
    BuildContext context,
    GameEntity game,
    List<dynamic> sortedParticipants,
    String currentUserId,
    bool isUpdatingSettings,
  ) {
    final isHost = game.isHost(currentUserId);

    return RefreshIndicator(
      color: AppColors.primary,
      backgroundColor: AppColors.surfaceContainer,
      onRefresh: () async {
        await ref.read(gameNotifierProvider.notifier).refreshGameDetails(widget.gameId);
      },
      child: SingleChildScrollView(
        physics: const AlwaysScrollableScrollPhysics(),
        padding: const EdgeInsets.symmetric(
          horizontal: AppSpacing.margin,
          vertical: AppSpacing.md,
        ),
        child: Column(
          crossAxisAlignment: CrossAxisAlignment.stretch,
          children: [
            // Tarjeta de Código de Partida
            _buildGameCodeCard(context, game),
            const SizedBox(height: AppSpacing.md),

            // Rol del usuario actual y estado de la partida
            _buildStatusHeader(game, isHost),
            const SizedBox(height: AppSpacing.lg),

            // Acceso a la ronda en curso (Game.state == IN_PROGRESS)
            if (game.isInProgress) ...[
              TappazoButton(
                text: 'Entrar a la ronda en curso',
                icon: Icons.casino_rounded,
                onPressed: _openGameScreen,
              ),
              const SizedBox(height: AppSpacing.lg),
            ],

            // Panel de Configuración de la Bebida (Solo Host en LOBBY puede editar)
            _buildDrinkSettingsCard(context, game, isHost, isUpdatingSettings),
            const SizedBox(height: AppSpacing.lg),

            // Lista de Participantes ordenados alfabéticamente (Sección 4 y 9)
            _buildParticipantsSection(sortedParticipants, game.hostId),
            const SizedBox(height: AppSpacing.xl),

            // Acceso directo a la Mesa de Deudas
            TappazoButton(
              text: 'Ver Mesa y Resumen de Deudas',
              icon: Icons.receipt_long_rounded,
              isOutlined: true,
              onPressed: () {
                Navigator.of(context).push(
                  MaterialPageRoute(builder: (_) => MesaScreen(gameId: widget.gameId)),
                );
              },
            ),
          ],
        ),
      ),
    );
  }

  Widget _buildGameCodeCard(BuildContext context, GameEntity game) {
    return Container(
      padding: const EdgeInsets.all(AppSpacing.md),
      decoration: BoxDecoration(
        color: AppColors.surfaceContainer,
        borderRadius: AppSpacing.borderRadiusMd,
        border: Border.all(color: AppColors.surfaceContainerHighest, width: 1),
      ),
      child: Column(
        children: [
          Text(
            'CÓDIGO DE PARTIDA',
            style: AppTypography.labelSm.copyWith(color: AppColors.textSecondary),
          ),
          const SizedBox(height: AppSpacing.xs),
          Row(
            mainAxisAlignment: MainAxisAlignment.center,
            children: [
              Text(
                game.code,
                style: AppTypography.capDisplay.copyWith(color: AppColors.primary),
              ),
              const SizedBox(width: AppSpacing.sm),
              IconButton(
                icon: const Icon(Icons.copy_rounded, color: AppColors.textSecondary),
                tooltip: 'Copiar código',
                onPressed: () {
                  Clipboard.setData(ClipboardData(text: game.code));
                  ScaffoldMessenger.of(context).showSnackBar(
                    SnackBar(
                      content: Text('Código ${game.code} copiado al portapapeles'),
                      behavior: SnackBarBehavior.floating,
                      backgroundColor: AppColors.surfaceContainerHighest,
                    ),
                  );
                },
              ),
            ],
          ),
          const SizedBox(height: AppSpacing.xs),
          Text(
            'Comparte este código para que tus amigos se unan',
            style: AppTypography.bodySm,
          ),
        ],
      ),
    );
  }

  Widget _buildStatusHeader(GameEntity game, bool isHost) {
    return Row(
      children: [
        // Badge Estado
        Container(
          padding: const EdgeInsets.symmetric(horizontal: 12, vertical: 6),
          decoration: BoxDecoration(
            color: game.isLobby
                ? AppColors.secondary.withValues(alpha: 0.15)
                : AppColors.primary.withValues(alpha: 0.15),
            borderRadius: AppSpacing.borderRadiusFull,
            border: Border.all(
              color: game.isLobby ? AppColors.secondary : AppColors.primary,
              width: 1,
            ),
          ),
          child: Row(
            mainAxisSize: MainAxisSize.min,
            children: [
              Container(
                width: 6,
                height: 6,
                decoration: BoxDecoration(
                  shape: BoxShape.circle,
                  color: game.isLobby ? AppColors.secondary : AppColors.primary,
                ),
              ),
              const SizedBox(width: 6),
              Text(
                game.state.displayName.toUpperCase(),
                style: AppTypography.labelSm.copyWith(
                  color: game.isLobby ? AppColors.secondary : AppColors.primary,
                ),
              ),
            ],
          ),
        ),
        const Spacer(),
        // Badge Rol
        Container(
          padding: const EdgeInsets.symmetric(horizontal: 12, vertical: 6),
          decoration: BoxDecoration(
            color: isHost
                ? AppColors.primary.withValues(alpha: 0.2)
                : AppColors.surfaceContainerHigh,
            borderRadius: AppSpacing.borderRadiusFull,
          ),
          child: Row(
            mainAxisSize: MainAxisSize.min,
            children: [
              Icon(
                isHost ? Icons.workspace_premium_rounded : Icons.person_rounded,
                size: 14,
                color: isHost ? AppColors.primary : AppColors.textSecondary,
              ),
              const SizedBox(width: 4),
              Text(
                isHost ? 'ERES EL ANFITRIÓN' : 'JUGADOR',
                style: AppTypography.labelSm.copyWith(
                  color: isHost ? AppColors.primary : AppColors.textSecondary,
                ),
              ),
            ],
          ),
        ),
      ],
    );
  }

  Widget _buildDrinkSettingsCard(
    BuildContext context,
    GameEntity game,
    bool isHost,
    bool isUpdatingSettings,
  ) {
    return Container(
      padding: const EdgeInsets.all(AppSpacing.md),
      decoration: BoxDecoration(
        color: AppColors.surfaceContainer,
        borderRadius: AppSpacing.borderRadiusMd,
        border: Border.all(color: AppColors.surfaceContainerHigh, width: 1),
      ),
      child: Column(
        crossAxisAlignment: CrossAxisAlignment.start,
        children: [
          Row(
            mainAxisAlignment: MainAxisAlignment.spaceBetween,
            children: [
              Text(
                'AJUSTES DE LA BEBIDA',
                style: AppTypography.labelMd.copyWith(color: AppColors.textSecondary),
              ),
              if (isHost && game.isLobby)
                TextButton.icon(
                  onPressed: isUpdatingSettings
                      ? null
                      : () {
                          setState(() {
                            if (!_isEditingSettings) {
                              _drinkPriceController.text = game.currentDrinkPrice.toString();
                              _selectedMode = game.currentMode;
                            }
                            _isEditingSettings = !_isEditingSettings;
                          });
                        },
                  icon: Icon(
                    _isEditingSettings ? Icons.close_rounded : Icons.edit_rounded,
                    size: 16,
                    color: AppColors.primary,
                  ),
                  label: Text(
                    _isEditingSettings ? 'Cancelar' : 'Modificar',
                    style: AppTypography.labelSm.copyWith(color: AppColors.primary),
                  ),
                )
              else
                Row(
                  mainAxisSize: MainAxisSize.min,
                  children: [
                    const Icon(Icons.lock_outline_rounded, size: 14, color: AppColors.outline),
                    const SizedBox(width: 4),
                    Text(
                      'Solo anfitrión',
                      style: AppTypography.labelSm.copyWith(color: AppColors.outline),
                    ),
                  ],
                ),
            ],
          ),
          const SizedBox(height: AppSpacing.sm),

          if (!_isEditingSettings) ...[
            Row(
              mainAxisAlignment: MainAxisAlignment.spaceBetween,
              children: [
                Column(
                  crossAxisAlignment: CrossAxisAlignment.start,
                  children: [
                    Text('Precio de la ronda', style: AppTypography.bodySm),
                    Text(
                      CurrencyFormatter.format(game.currentDrinkPrice),
                      style: AppTypography.currencyDisplay.copyWith(color: AppColors.primary),
                    ),
                  ],
                ),
                Column(
                  crossAxisAlignment: CrossAxisAlignment.end,
                  children: [
                    Text('Modalidad', style: AppTypography.bodySm),
                    Text(
                      game.currentMode.displayName,
                      style: AppTypography.labelMd.copyWith(color: AppColors.textPrimary),
                    ),
                    Text(
                      'Mín. ${game.currentMode.minPlayers} jugadores',
                      style: AppTypography.bodySm.copyWith(color: AppColors.outline),
                    ),
                  ],
                ),
              ],
            ),
          ] else ...[
            // Formulario de edición para el Host
            TextField(
              controller: _drinkPriceController,
              keyboardType: TextInputType.number,
              decoration: const InputDecoration(
                labelText: 'Precio de la bebida (COP)',
                prefixText: '\$ ',
              ),
            ),
            const SizedBox(height: AppSpacing.md),
            Text('Modalidad de juego', style: AppTypography.bodySm),
            const SizedBox(height: AppSpacing.xs),
            DropdownButtonFormField<GameMode>(
              value: _selectedMode ?? game.currentMode,
              dropdownColor: AppColors.surfaceContainerHigh,
              items: GameMode.values.map((mode) {
                return DropdownMenuItem(
                  value: mode,
                  child: Text(
                    '${mode.displayName} (Mín. ${mode.minPlayers})',
                    style: AppTypography.bodyMd,
                  ),
                );
              }).toList(),
              onChanged: (newMode) {
                if (newMode != null) {
                  setState(() => _selectedMode = newMode);
                }
              },
            ),
            const SizedBox(height: AppSpacing.md),
            TappazoButton(
              text: 'Guardar Ajustes',
              isLoading: isUpdatingSettings,
              onPressed: () async {
                final messenger = ScaffoldMessenger.of(context);
                final price = int.tryParse(_drinkPriceController.text.trim());
                if (price == null || price <= 0) {
                  messenger.showSnackBar(
                    const SnackBar(
                      content: Text('Por favor ingresa un precio válido mayor a 0'),
                      backgroundColor: AppColors.errorContainer,
                    ),
                  );
                  return;
                }

                final success = await ref.read(gameNotifierProvider.notifier).updateSettings(
                      gameId: game.id,
                      drinkPrice: price,
                      mode: _selectedMode,
                    );

                if (success && mounted) {
                  setState(() => _isEditingSettings = false);
                  messenger.showSnackBar(
                    const SnackBar(
                      content: Text('Ajustes actualizados correctamente'),
                      backgroundColor: AppColors.surfaceContainerHighest,
                    ),
                  );
                }
              },
            ),
          ],
        ],
      ),
    );
  }

  Widget _buildParticipantsSection(List<dynamic> participants, String hostId) {
    return Column(
      crossAxisAlignment: CrossAxisAlignment.start,
      children: [
        Row(
          mainAxisAlignment: MainAxisAlignment.spaceBetween,
          children: [
            Text('JUGADORES EN SALA', style: AppTypography.labelMd),
            Text(
              '${participants.length} participantes',
              style: AppTypography.bodySm.copyWith(color: AppColors.textSecondary),
            ),
          ],
        ),
        const SizedBox(height: AppSpacing.sm),
        if (participants.isEmpty)
          Container(
            padding: const EdgeInsets.all(AppSpacing.md),
            decoration: BoxDecoration(
              color: AppColors.surfaceContainerLow,
              borderRadius: AppSpacing.borderRadiusMd,
            ),
            child: Center(
              child: Text(
                'Esperando a que se unan jugadores...',
                style: AppTypography.bodySm.copyWith(color: AppColors.outline),
              ),
            ),
          )
        else
          ListView.separated(
            shrinkWrap: true,
            physics: const NeverScrollableScrollPhysics(),
            itemCount: participants.length,
            separatorBuilder: (_, __) => const SizedBox(height: AppSpacing.xs),
            itemBuilder: (context, index) {
              final participant = participants[index];
              final isPlayerHost = participant.userId == hostId;

              return Container(
                padding: const EdgeInsets.symmetric(
                  horizontal: AppSpacing.md,
                  vertical: AppSpacing.sm,
                ),
                decoration: BoxDecoration(
                  color: AppColors.surfaceContainer,
                  borderRadius: AppSpacing.borderRadiusMd,
                  border: Border.all(
                    color: isPlayerHost
                        ? AppColors.primary.withValues(alpha: 0.3)
                        : AppColors.surfaceContainerHigh,
                    width: 1,
                  ),
                ),
                child: Row(
                  children: [
                    CircleAvatar(
                      radius: 18,
                      backgroundColor: isPlayerHost ? AppColors.primary : AppColors.surfaceBright,
                      child: Text(
                        participant.displayName.isNotEmpty
                            ? participant.displayName[0].toUpperCase()
                            : '?',
                        style: AppTypography.labelMd.copyWith(
                          color: isPlayerHost ? AppColors.onPrimaryContainer : AppColors.textPrimary,
                        ),
                      ),
                    ),
                    const SizedBox(width: AppSpacing.md),
                    Expanded(
                      child: Column(
                        crossAxisAlignment: CrossAxisAlignment.start,
                        children: [
                          Row(
                            children: [
                              Flexible(
                                child: Text(
                                  participant.displayName,
                                  overflow: TextOverflow.ellipsis,
                                  style: AppTypography.labelMd,
                                ),
                              ),
                              if (isPlayerHost) ...[
                                const SizedBox(width: 6),
                                const Icon(Icons.star_rounded, size: 16, color: AppColors.primary),
                              ],
                            ],
                          ),
                          Text(
                            participant.role,
                            style: AppTypography.bodySm.copyWith(color: AppColors.outline),
                          ),
                        ],
                      ),
                    ),
                    Container(
                      padding: const EdgeInsets.symmetric(horizontal: 8, vertical: 4),
                      decoration: BoxDecoration(
                        color: participant.isActive
                            ? AppColors.secondary.withValues(alpha: 0.1)
                            : AppColors.surfaceContainerHighest,
                        borderRadius: AppSpacing.borderRadiusFull,
                      ),
                      child: Text(
                        participant.participantState,
                        style: AppTypography.labelSm.copyWith(
                          color: participant.isActive ? AppColors.secondary : AppColors.outline,
                        ),
                      ),
                    ),
                  ],
                ),
              );
            },
          ),
      ],
    );
  }
}
