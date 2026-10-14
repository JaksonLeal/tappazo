import 'package:flutter/material.dart';
import 'package:flutter_riverpod/flutter_riverpod.dart';
import '../../providers/auth_providers.dart';
import '../../providers/auth_state.dart';
import '../../theme/colors.dart';
import '../../theme/spacing.dart';
import '../../theme/typography.dart';
import '../../widgets/google_sign_in_button.dart';
import '../../widgets/tappazo_button.dart';
import '../home/home_screen.dart';

class LoginScreen extends ConsumerStatefulWidget {
  const LoginScreen({super.key});

  @override
  ConsumerState<LoginScreen> createState() => _LoginScreenState();
}

class _LoginScreenState extends ConsumerState<LoginScreen> {
  final _emailController = TextEditingController(text: 'jugador@tappazo.com');
  final _nicknameController = TextEditingController(text: 'Tappero');
  bool _showDevMode = false;

  @override
  void dispose() {
    _emailController.dispose();
    _nicknameController.dispose();
    super.dispose();
  }

  @override
  Widget build(BuildContext context) {
    final authState = ref.watch(authNotifierProvider);
    final isLoading = authState is AuthLoading;

    ref.listen<AuthState>(authNotifierProvider, (previous, next) {
      if (next is Authenticated) {
        Navigator.of(context).pushReplacement(
          MaterialPageRoute(builder: (_) => const HomeScreen()),
        );
      } else if (next is AuthError) {
        ScaffoldMessenger.of(context).showSnackBar(
          SnackBar(
            content: Text(next.message),
            backgroundColor: AppColors.errorContainer,
            behavior: SnackBarBehavior.floating,
          ),
        );
      }
    });

    return Scaffold(
      backgroundColor: AppColors.background,
      body: SafeArea(
        child: Center(
          child: SingleChildScrollView(
            padding: const EdgeInsets.symmetric(
              horizontal: AppSpacing.margin,
              vertical: AppSpacing.lg,
            ),
            child: Column(
              mainAxisAlignment: MainAxisAlignment.center,
              crossAxisAlignment: CrossAxisAlignment.stretch,
              children: [
                // Logo & Tapa Cervecera de Tappazo
                Center(
                  child: Container(
                    width: 96,
                    height: 96,
                    decoration: BoxDecoration(
                      shape: BoxShape.circle,
                      gradient: const LinearGradient(
                        colors: [AppColors.secondary, AppColors.primary],
                        begin: Alignment.topLeft,
                        end: Alignment.bottomRight,
                      ),
                      boxShadow: [
                        BoxShadow(
                          color: AppColors.primary.withValues(alpha: 0.3),
                          blurRadius: 24,
                          spreadRadius: 2,
                        ),
                      ],
                    ),
                    child: const Icon(
                      Icons.sports_bar_rounded,
                      size: 48,
                      color: AppColors.onPrimaryContainer,
                    ),
                  ),
                ),
                const SizedBox(height: AppSpacing.lg),

                // Títulos
                Text(
                  'TAPPAZO',
                  textAlign: TextAlign.center,
                  style: AppTypography.headlineXl.copyWith(color: AppColors.primary),
                ),
                const SizedBox(height: AppSpacing.xs),
                Text(
                  'Inicia sesión para crear partidas,\nrevelar tus tapas y jugar con amigos',
                  textAlign: TextAlign.center,
                  style: AppTypography.bodyMd.copyWith(color: AppColors.textSecondary),
                ),
                const SizedBox(height: AppSpacing.xl),

                // Botón oficial de Google Sign-In
                GoogleSignInButton(
                  isLoading: isLoading && !_showDevMode,
                  onPressed: isLoading
                      ? null
                      : () {
                          ref.read(authNotifierProvider.notifier).signInWithGoogle();
                        },
                ),
                const SizedBox(height: AppSpacing.lg),

                // Separador o Toggle de Modo Desarrollo
                Row(
                  children: [
                    const Expanded(child: Divider(color: AppColors.surfaceContainerHigh)),
                    Padding(
                      padding: const EdgeInsets.symmetric(horizontal: AppSpacing.sm),
                      child: TextButton.icon(
                        onPressed: () {
                          setState(() {
                            _showDevMode = !_showDevMode;
                          });
                        },
                        icon: Icon(
                          _showDevMode
                              ? Icons.keyboard_arrow_up
                              : Icons.keyboard_arrow_down,
                          size: 18,
                          color: AppColors.outline,
                        ),
                        label: Text(
                          _showDevMode
                              ? 'Ocultar login de pruebas'
                              : 'Probar sin Google (Backend Local)',
                          style: AppTypography.bodySm.copyWith(color: AppColors.outline),
                        ),
                      ),
                    ),
                    const Expanded(child: Divider(color: AppColors.surfaceContainerHigh)),
                  ],
                ),

                // Formulario de login local (Spring local-auth)
                if (_showDevMode) ...[
                  const SizedBox(height: AppSpacing.md),
                  Container(
                    padding: const EdgeInsets.all(AppSpacing.md),
                    decoration: BoxDecoration(
                      color: AppColors.surfaceContainer,
                      borderRadius: AppSpacing.borderRadiusMd,
                      border: Border.all(
                        color: AppColors.surfaceContainerHigh,
                        width: 1,
                      ),
                    ),
                    child: Column(
                      crossAxisAlignment: CrossAxisAlignment.stretch,
                      children: [
                        Text(
                          'Simulación Local (POST /auth/local)',
                          style: AppTypography.labelMd.copyWith(color: AppColors.primary),
                        ),
                        const SizedBox(height: AppSpacing.md),
                        TextField(
                          controller: _nicknameController,
                          decoration: const InputDecoration(
                            labelText: 'Nickname',
                            prefixIcon: Icon(Icons.person_outline, size: 20),
                          ),
                        ),
                        const SizedBox(height: AppSpacing.sm),
                        TextField(
                          controller: _emailController,
                          keyboardType: TextInputType.emailAddress,
                          decoration: const InputDecoration(
                            labelText: 'Correo electrónico',
                            prefixIcon: Icon(Icons.email_outlined, size: 20),
                          ),
                        ),
                        const SizedBox(height: AppSpacing.md),
                        TappazoButton(
                          text: 'Entrar en Modo Local',
                          isLoading: isLoading && _showDevMode,
                          onPressed: () {
                            ref.read(authNotifierProvider.notifier).signInWithLocal(
                                  email: _emailController.text,
                                  nickname: _nicknameController.text,
                                );
                          },
                        ),
                      ],
                    ),
                  ),
                ],
              ],
            ),
          ),
        ),
      ),
    );
  }
}
