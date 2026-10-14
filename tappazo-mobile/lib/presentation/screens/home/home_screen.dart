import 'package:flutter/material.dart';
import 'package:flutter_riverpod/flutter_riverpod.dart';
import '../../../core/constants/api_constants.dart';
import '../../../domain/entities/game_mode.dart';
import '../../providers/auth_providers.dart';
import '../../providers/auth_state.dart';
import '../../providers/game_providers.dart';
import '../../providers/game_state.dart';
import '../../theme/colors.dart';
import '../../theme/spacing.dart';
import '../../theme/typography.dart';
import '../../widgets/tappazo_button.dart';
import '../auth/login_screen.dart';
import '../lobby/lobby_screen.dart';

class HomeScreen extends ConsumerStatefulWidget {
  const HomeScreen({super.key});

  @override
  ConsumerState<HomeScreen> createState() => _HomeScreenState();
}

class _HomeScreenState extends ConsumerState<HomeScreen> {
  @override
  Widget build(BuildContext context) {
    final authState = ref.watch(authNotifierProvider);

    ref.listen<AuthState>(authNotifierProvider, (previous, next) {
      if (next is Unauthenticated) {
        Navigator.of(context).pushReplacement(
          MaterialPageRoute(builder: (_) => const LoginScreen()),
        );
      }
    });

    final user = authState is Authenticated ? authState.user : null;

    return Scaffold(
      backgroundColor: AppColors.background,
      appBar: AppBar(
        title: Text('TAPPAZO', style: AppTypography.headlineSm),
        actions: [
          IconButton(
            icon: const Icon(Icons.logout_rounded, color: AppColors.error),
            tooltip: 'Cerrar sesión',
            onPressed: () => _showLogoutDialog(context),
          ),
        ],
      ),
      body: SafeArea(
        child: SingleChildScrollView(
          padding: const EdgeInsets.symmetric(
            horizontal: AppSpacing.margin,
            vertical: AppSpacing.md,
          ),
          child: Column(
            crossAxisAlignment: CrossAxisAlignment.stretch,
            children: [
              // Tarjeta de perfil de usuario autenticado
              _buildUserProfileCard(user),
              const SizedBox(height: AppSpacing.md),

              // Información de conexión con Backend
              _buildServerStatusCard(),
              const SizedBox(height: AppSpacing.xl),

              // Sección de Acciones de Partidas
              Text('PARTIDAS', style: AppTypography.labelMd),
              const SizedBox(height: AppSpacing.sm),

              TappazoButton(
                text: 'Crear Partida',
                icon: Icons.add_circle_outline_rounded,
                onPressed: () => _showCreateGameDialog(context),
              ),
              const SizedBox(height: AppSpacing.md),

              TappazoButton(
                text: 'Unirse con Código',
                icon: Icons.qr_code_rounded,
                isOutlined: true,
                onPressed: () => _showJoinGameDialog(context),
              ),
              const SizedBox(height: AppSpacing.xl),

              // Botón de salir
              OutlinedButton.icon(
                onPressed: () => _showLogoutDialog(context),
                icon: const Icon(Icons.logout, size: 18, color: AppColors.error),
                label: Text(
                  'Cerrar Sesión',
                  style: AppTypography.labelMd.copyWith(color: AppColors.error),
                ),
                style: OutlinedButton.styleFrom(
                  side: const BorderSide(color: AppColors.errorContainer, width: 1),
                  minimumSize: const Size.fromHeight(AppSpacing.buttonPrimaryHeight),
                  shape: RoundedRectangleBorder(
                    borderRadius: AppSpacing.borderRadiusMd,
                  ),
                ),
              ),
            ],
          ),
        ),
      ),
    );
  }

  Widget _buildUserProfileCard(dynamic user) {
    return Container(
      padding: const EdgeInsets.all(AppSpacing.md),
      decoration: BoxDecoration(
        color: AppColors.surfaceContainer,
        borderRadius: AppSpacing.borderRadiusMd,
        border: Border.all(
          color: AppColors.surfaceContainerHigh,
          width: 1,
        ),
      ),
      child: Row(
        children: [
          CircleAvatar(
            radius: AppSpacing.avatarSize / 2,
            backgroundColor: AppColors.primary,
            child: Text(
              (user?.nickname.isNotEmpty == true) ? user!.nickname[0].toUpperCase() : 'T',
              style: AppTypography.headlineSm.copyWith(color: AppColors.onPrimaryContainer),
            ),
          ),
          const SizedBox(width: AppSpacing.md),
          Expanded(
            child: Column(
              crossAxisAlignment: CrossAxisAlignment.start,
              children: [
                Text(
                  user?.nickname ?? 'Jugador',
                  style: AppTypography.headlineSm,
                ),
                const SizedBox(height: 2),
                Text(
                  user?.email ?? '',
                  style: AppTypography.bodySm,
                ),
                const SizedBox(height: 6),
                Row(
                  children: [
                    Container(
                      width: 8,
                      height: 8,
                      decoration: const BoxDecoration(
                        color: AppColors.secondary,
                        shape: BoxShape.circle,
                      ),
                    ),
                    const SizedBox(width: 6),
                    Text(
                      'JWT Tappazo activo',
                      style: AppTypography.labelSm.copyWith(color: AppColors.secondary),
                    ),
                  ],
                ),
              ],
            ),
          ),
        ],
      ),
    );
  }

  Widget _buildServerStatusCard() {
    return Container(
      padding: const EdgeInsets.all(AppSpacing.md),
      decoration: BoxDecoration(
        color: AppColors.surfaceContainerLow,
        borderRadius: AppSpacing.borderRadiusMd,
        border: Border.all(
          color: AppColors.surfaceContainerHigh,
          width: 0.8,
        ),
      ),
      child: Column(
        crossAxisAlignment: CrossAxisAlignment.start,
        children: [
          Row(
            children: [
              const Icon(Icons.link_rounded, size: 18, color: AppColors.secondary),
              const SizedBox(width: AppSpacing.xs),
              Text(
                'Servidor Backend Conectado',
                style: AppTypography.labelSm.copyWith(color: AppColors.secondary),
              ),
            ],
          ),
          const SizedBox(height: AppSpacing.xs),
          Text(
            ApiConstants.baseUrl,
            style: AppTypography.bodySm.copyWith(
              fontFamily: 'monospace',
              color: AppColors.outline,
            ),
          ),
        ],
      ),
    );
  }

  void _showCreateGameDialog(BuildContext context) {
    final priceController = TextEditingController(text: '4000');
    GameMode selectedMode = GameMode.ultimoPierde;
    int maxPlayers = 8;
    bool isSubmitting = false;

    showModalBottomSheet(
      context: context,
      isScrollControlled: true,
      backgroundColor: AppColors.surfaceContainer,
      shape: const RoundedRectangleBorder(
        borderRadius: BorderRadius.vertical(top: Radius.circular(20)),
      ),
      builder: (ctx) {
        return StatefulBuilder(
          builder: (modalContext, setModalState) {
            return Padding(
              padding: EdgeInsets.only(
                left: AppSpacing.margin,
                right: AppSpacing.margin,
                top: AppSpacing.lg,
                bottom: MediaQuery.of(modalContext).viewInsets.bottom + AppSpacing.lg,
              ),
              child: SingleChildScrollView(
                child: Column(
                  mainAxisSize: MainAxisSize.min,
                  crossAxisAlignment: CrossAxisAlignment.stretch,
                  children: [
                    Text('CREAR NUEVA PARTIDA', style: AppTypography.headlineSm),
                    const SizedBox(height: AppSpacing.xs),
                    Text(
                      'Configura la ronda inicial y la capacidad (Sección 67)',
                      style: AppTypography.bodySm,
                    ),
                    const SizedBox(height: AppSpacing.lg),

                    TextField(
                      controller: priceController,
                      keyboardType: TextInputType.number,
                      decoration: const InputDecoration(
                        labelText: 'Precio de la bebida (COP)',
                        prefixText: '\$ ',
                      ),
                    ),
                    const SizedBox(height: AppSpacing.md),

                    Text('Modalidad de Juego', style: AppTypography.bodySm),
                    const SizedBox(height: AppSpacing.xs),
                    DropdownButtonFormField<GameMode>(
                      initialValue: selectedMode,
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
                          setModalState(() {
                            selectedMode = newMode;
                            if (maxPlayers < selectedMode.minPlayers) {
                              maxPlayers = selectedMode.minPlayers;
                            }
                          });
                        }
                      },
                    ),
                    const SizedBox(height: AppSpacing.md),

                    Row(
                      mainAxisAlignment: MainAxisAlignment.spaceBetween,
                      children: [
                        Text('Límite de jugadores', style: AppTypography.bodySm),
                        Text(
                          '$maxPlayers jugadores (Mín. ${selectedMode.minPlayers})',
                          style: AppTypography.labelMd.copyWith(color: AppColors.primary),
                        ),
                      ],
                    ),
                    Slider(
                      value: maxPlayers.toDouble(),
                      min: selectedMode.minPlayers.toDouble(),
                      max: 10,
                      divisions: 10 - selectedMode.minPlayers,
                      activeColor: AppColors.primary,
                      inactiveColor: AppColors.surfaceContainerHighest,
                      onChanged: (val) {
                        setModalState(() => maxPlayers = val.toInt());
                      },
                    ),
                    const SizedBox(height: AppSpacing.lg),

                    TappazoButton(
                      text: 'Crear Partida y Abrir Sala',
                      isLoading: isSubmitting,
                      onPressed: () async {
                        final price = int.tryParse(priceController.text.trim());
                        if (price == null || price <= 0) {
                          ScaffoldMessenger.of(modalContext).showSnackBar(
                            const SnackBar(
                              content: Text('Ingresa un precio válido mayor a 0'),
                              backgroundColor: AppColors.errorContainer,
                            ),
                          );
                          return;
                        }

                        setModalState(() => isSubmitting = true);
                        final navigator = Navigator.of(context);
                        final messenger = ScaffoldMessenger.of(context);

                        final game = await ref.read(gameNotifierProvider.notifier).createGame(
                              drinkPrice: price,
                              initialMode: selectedMode,
                              maxPlayers: maxPlayers,
                            );

                        if (game != null) {
                          navigator.pop();
                          navigator.push(
                            MaterialPageRoute(
                              builder: (_) => LobbyScreen(gameId: game.id),
                            ),
                          );
                        } else {
                          setModalState(() => isSubmitting = false);
                          messenger.showSnackBar(
                            const SnackBar(
                              content: Text('Error al crear la partida'),
                              backgroundColor: AppColors.errorContainer,
                            ),
                          );
                        }
                      },
                    ),
                  ],
                ),
              ),
            );
          },
        );
      },
    );
  }

  void _showJoinGameDialog(BuildContext context) {
    final codeController = TextEditingController();
    bool isSubmitting = false;

    showModalBottomSheet(
      context: context,
      isScrollControlled: true,
      backgroundColor: AppColors.surfaceContainer,
      shape: const RoundedRectangleBorder(
        borderRadius: BorderRadius.vertical(top: Radius.circular(20)),
      ),
      builder: (ctx) {
        return StatefulBuilder(
          builder: (modalContext, setModalState) {
            return Padding(
              padding: EdgeInsets.only(
                left: AppSpacing.margin,
                right: AppSpacing.margin,
                top: AppSpacing.lg,
                bottom: MediaQuery.of(modalContext).viewInsets.bottom + AppSpacing.lg,
              ),
              child: Column(
                mainAxisSize: MainAxisSize.min,
                crossAxisAlignment: CrossAxisAlignment.stretch,
                children: [
                  Text('UNIRSE A UNA PARTIDA', style: AppTypography.headlineSm),
                  const SizedBox(height: AppSpacing.xs),
                  Text(
                    'Ingresa el código alfanumérico proporcionado por el anfitrión',
                    style: AppTypography.bodySm,
                  ),
                  const SizedBox(height: AppSpacing.lg),
                  TextField(
                    controller: codeController,
                    textCapitalization: TextCapitalization.characters,
                    decoration: const InputDecoration(
                      labelText: 'Código de Partida (ej. X7K29P)',
                      prefixIcon: Icon(Icons.vpn_key_rounded, size: 20),
                    ),
                  ),
                  const SizedBox(height: AppSpacing.lg),
                  TappazoButton(
                    text: 'Entrar a la Sala',
                    isLoading: isSubmitting,
                    onPressed: () async {
                      final code = codeController.text.trim().toUpperCase();
                      if (code.length < 4 || code.length > 10) {
                        ScaffoldMessenger.of(modalContext).showSnackBar(
                          const SnackBar(
                            content: Text('El código debe tener entre 4 y 10 caracteres'),
                            backgroundColor: AppColors.errorContainer,
                          ),
                        );
                        return;
                      }

                      setModalState(() => isSubmitting = true);
                      final navigator = Navigator.of(context);
                      final messenger = ScaffoldMessenger.of(context);

                      final success = await ref.read(gameNotifierProvider.notifier).joinGame(code);

                      if (success) {
                        navigator.pop();
                        final gameState = ref.read(gameNotifierProvider);
                        if (gameState is GameLoaded) {
                          navigator.push(
                            MaterialPageRoute(
                              builder: (_) => LobbyScreen(gameId: gameState.game.id),
                            ),
                          );
                        }
                      } else {
                        setModalState(() => isSubmitting = false);
                        final state = ref.read(gameNotifierProvider);
                        final errorMsg = state is GameError ? state.message : 'Error al unirse a la partida';
                        messenger.showSnackBar(
                          SnackBar(
                            content: Text(errorMsg),
                            backgroundColor: AppColors.errorContainer,
                          ),
                        );
                      }
                    },
                  ),
                ],
              ),
            );
          },
        );
      },
    );
  }

  void _showLogoutDialog(BuildContext context) {
    showDialog(
      context: context,
      builder: (ctx) => AlertDialog(
        backgroundColor: AppColors.surfaceContainer,
        title: Text('Cerrar sesión', style: AppTypography.headlineSm),
        content: Text(
          '¿Estás seguro de que deseas salir de Tappazo? Deberás iniciar sesión de nuevo.',
          style: AppTypography.bodyMd,
        ),
        actions: [
          TextButton(
            onPressed: () => Navigator.of(ctx).pop(),
            child: Text('Cancelar', style: AppTypography.labelMd),
          ),
          ElevatedButton(
            style: ElevatedButton.styleFrom(
              backgroundColor: AppColors.errorContainer,
              foregroundColor: AppColors.textPrimary,
            ),
            onPressed: () {
              Navigator.of(ctx).pop();
              ref.read(authNotifierProvider.notifier).signOut();
            },
            child: Text('Salir', style: AppTypography.labelMd),
          ),
        ],
      ),
    );
  }
}
