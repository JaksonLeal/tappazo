import 'package:flutter/material.dart';
import 'package:flutter_riverpod/flutter_riverpod.dart';
import '../../../core/utils/currency_formatter.dart';
import '../../providers/mesa_providers.dart';
import '../../providers/mesa_state.dart';
import '../../theme/colors.dart';
import '../../theme/spacing.dart';
import '../../theme/typography.dart';
import '../../widgets/tappazo_button.dart';

class MesaScreen extends ConsumerStatefulWidget {
  final String gameId;

  const MesaScreen({super.key, required this.gameId});

  @override
  ConsumerState<MesaScreen> createState() => _MesaScreenState();
}

class _MesaScreenState extends ConsumerState<MesaScreen> with SingleTickerProviderStateMixin {
  late TabController _tabController;

  @override
  void initState() {
    super.initState();
    _tabController = TabController(length: 2, vsync: this);
    WidgetsBinding.instance.addPostFrameCallback((_) {
      ref.read(mesaNotifierProvider.notifier).loadMesaData(widget.gameId);
    });
  }

  @override
  void dispose() {
    _tabController.dispose();
    super.dispose();
  }

  @override
  Widget build(BuildContext context) {
    final mesaState = ref.watch(mesaNotifierProvider);

    return Scaffold(
      backgroundColor: AppColors.background,
      appBar: AppBar(
        title: Text('MESA Y BALANCES', style: AppTypography.headlineSm),
        actions: [
          IconButton(
            icon: const Icon(Icons.refresh_rounded),
            tooltip: 'Actualizar mesa',
            onPressed: () {
              ref.read(mesaNotifierProvider.notifier).refresh(widget.gameId);
            },
          ),
        ],
        bottom: TabBar(
          controller: _tabController,
          indicatorColor: AppColors.primary,
          indicatorWeight: 3,
          labelColor: AppColors.primary,
          unselectedLabelColor: AppColors.textSecondary,
          labelStyle: AppTypography.labelMd,
          tabs: const [
            Tab(text: 'Saldos Netos'),
            Tab(text: 'Movimientos Brutos'),
          ],
        ),
      ),
      body: switch (mesaState) {
        MesaLoading(:final message) => Center(
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
        MesaError(:final message) => Center(
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
                      ref.read(mesaNotifierProvider.notifier).loadMesaData(widget.gameId);
                    },
                  ),
                ],
              ),
            ),
          ),
        MesaLoaded(:final mesa, :final balance, :final debts) => TabBarView(
            controller: _tabController,
            children: [
              _buildNetDebtsTab(context, mesa, balance),
              _buildDebtMovementsTab(context, debts),
            ],
          ),
        _ => const SizedBox.shrink(),
      },
    );
  }

  Widget _buildNetDebtsTab(BuildContext context, dynamic mesa, dynamic balance) {
    final netDebts = balance.netDebts as List<dynamic>;

    return RefreshIndicator(
      color: AppColors.primary,
      backgroundColor: AppColors.surfaceContainer,
      onRefresh: () => ref.read(mesaNotifierProvider.notifier).refresh(widget.gameId),
      child: SingleChildScrollView(
        physics: const AlwaysScrollableScrollPhysics(),
        padding: const EdgeInsets.symmetric(
          horizontal: AppSpacing.margin,
          vertical: AppSpacing.md,
        ),
        child: Column(
          crossAxisAlignment: CrossAxisAlignment.stretch,
          children: [
            // Tarjeta de Deuda Total Activa en la partida
            Container(
              padding: const EdgeInsets.all(AppSpacing.md),
              decoration: BoxDecoration(
                gradient: const LinearGradient(
                  colors: [
                    AppColors.surfaceContainerHigh,
                    AppColors.surfaceContainer,
                  ],
                  begin: Alignment.topLeft,
                  end: Alignment.bottomRight,
                ),
                borderRadius: AppSpacing.borderRadiusMd,
                border: Border.all(color: AppColors.surfaceContainerHighest, width: 1),
              ),
              child: Column(
                crossAxisAlignment: CrossAxisAlignment.start,
                children: [
                  Row(
                    mainAxisAlignment: MainAxisAlignment.spaceBetween,
                    children: [
                      Text(
                        'DEUDA TOTAL ACTIVA',
                        style: AppTypography.labelSm.copyWith(color: AppColors.textSecondary),
                      ),
                      Text(
                        'MESA ${mesa.code}',
                        style: AppTypography.labelSm.copyWith(color: AppColors.primary),
                      ),
                    ],
                  ),
                  const SizedBox(height: AppSpacing.xs),
                  Text(
                    CurrencyFormatter.format(balance.totalActiveDebt),
                    style: AppTypography.headlineLg.copyWith(color: AppColors.primary),
                  ),
                  const SizedBox(height: AppSpacing.xs),
                  Text(
                    'Monto neto total pendiente entre jugadores (Sección 51)',
                    style: AppTypography.bodySm,
                  ),
                ],
              ),
            ),
            const SizedBox(height: AppSpacing.lg),

            // Título de sección
            Row(
              mainAxisAlignment: MainAxisAlignment.spaceBetween,
              children: [
                Text('QUIÉN LE DEBE A QUIÉN', style: AppTypography.labelMd),
                Text(
                  '${netDebts.length} compromisos',
                  style: AppTypography.bodySm.copyWith(color: AppColors.textSecondary),
                ),
              ],
            ),
            const SizedBox(height: AppSpacing.sm),

            if (netDebts.isEmpty)
              Container(
                padding: const EdgeInsets.all(AppSpacing.lg),
                decoration: BoxDecoration(
                  color: AppColors.surfaceContainerLow,
                  borderRadius: AppSpacing.borderRadiusMd,
                ),
                child: Column(
                  children: [
                    const Icon(
                      Icons.check_circle_outline_rounded,
                      size: 40,
                      color: AppColors.secondary,
                    ),
                    const SizedBox(height: AppSpacing.sm),
                    Text(
                      '¡Cuentas al día!',
                      style: AppTypography.headlineSm.copyWith(color: AppColors.secondary),
                    ),
                    const SizedBox(height: AppSpacing.xs),
                    Text(
                      'No hay deudas activas registradas en esta partida aún.',
                      textAlign: TextAlign.center,
                      style: AppTypography.bodySm.copyWith(color: AppColors.outline),
                    ),
                  ],
                ),
              )
            else
              ListView.separated(
                shrinkWrap: true,
                physics: const NeverScrollableScrollPhysics(),
                itemCount: netDebts.length,
                separatorBuilder: (_, __) => const SizedBox(height: AppSpacing.sm),
                itemBuilder: (context, index) {
                  final item = netDebts[index];

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
                        // Deudor
                        Expanded(
                          child: Column(
                            crossAxisAlignment: CrossAxisAlignment.start,
                            children: [
                              Text(
                                'Debe',
                                style: AppTypography.labelSm.copyWith(color: AppColors.error),
                              ),
                              Text(
                                item.fromPlayerDisplay,
                                style: AppTypography.labelMd,
                                overflow: TextOverflow.ellipsis,
                              ),
                            ],
                          ),
                        ),
                        // Flecha y Monto
                        Column(
                          children: [
                            const Icon(
                              Icons.arrow_forward_rounded,
                              size: 20,
                              color: AppColors.primary,
                            ),
                            const SizedBox(height: 2),
                            Text(
                              CurrencyFormatter.format(item.amount),
                              style: AppTypography.labelMd.copyWith(color: AppColors.primary),
                            ),
                          ],
                        ),
                        // Acreedor
                        Expanded(
                          child: Column(
                            crossAxisAlignment: CrossAxisAlignment.end,
                            children: [
                              Text(
                                'A favor de',
                                style: AppTypography.labelSm.copyWith(color: AppColors.secondary),
                              ),
                              Text(
                                item.toPlayerDisplay,
                                style: AppTypography.labelMd,
                                overflow: TextOverflow.ellipsis,
                              ),
                            ],
                          ),
                        ),
                      ],
                    ),
                  );
                },
              ),
            const SizedBox(height: AppSpacing.md),
            Container(
              padding: const EdgeInsets.all(AppSpacing.sm),
              decoration: BoxDecoration(
                color: AppColors.surfaceContainerLowest,
                borderRadius: AppSpacing.borderRadiusSm,
              ),
              child: Text(
                'Nota: Tappazo no procesa dinero real. Los pagos se realizan externamente por transferencia o efectivo (Sección 1 y 50).',
                textAlign: TextAlign.center,
                style: AppTypography.bodySm.copyWith(color: AppColors.outline),
              ),
            ),
          ],
        ),
      ),
    );
  }

  Widget _buildDebtMovementsTab(BuildContext context, List<dynamic> debts) {
    return RefreshIndicator(
      color: AppColors.primary,
      backgroundColor: AppColors.surfaceContainer,
      onRefresh: () => ref.read(mesaNotifierProvider.notifier).refresh(widget.gameId),
      child: debts.isEmpty
          ? Center(
              child: Padding(
                padding: const EdgeInsets.all(AppSpacing.margin),
                child: Text(
                  'No se han registrado movimientos brutos de ronda en esta partida.',
                  textAlign: TextAlign.center,
                  style: AppTypography.bodySm.copyWith(color: AppColors.outline),
                ),
              ),
            )
          : ListView.separated(
              padding: const EdgeInsets.symmetric(
                horizontal: AppSpacing.margin,
                vertical: AppSpacing.md,
              ),
              itemCount: debts.length,
              separatorBuilder: (_, __) => const SizedBox(height: AppSpacing.sm),
              itemBuilder: (context, index) {
                final debt = debts[index];

                return Container(
                  padding: const EdgeInsets.all(AppSpacing.md),
                  decoration: BoxDecoration(
                    color: AppColors.surfaceContainer,
                    borderRadius: AppSpacing.borderRadiusMd,
                    border: Border.all(color: AppColors.surfaceContainerHigh, width: 1),
                  ),
                  child: Row(
                    children: [
                      Container(
                        padding: const EdgeInsets.all(AppSpacing.sm),
                        decoration: BoxDecoration(
                          color: AppColors.surfaceContainerHigh,
                          borderRadius: AppSpacing.borderRadiusSm,
                        ),
                        child: const Icon(
                          Icons.receipt_rounded,
                          size: 22,
                          color: AppColors.primary,
                        ),
                      ),
                      const SizedBox(width: AppSpacing.md),
                      Expanded(
                        child: Column(
                          crossAxisAlignment: CrossAxisAlignment.start,
                          children: [
                            Text(
                              debt.reasonDisplay,
                              style: AppTypography.labelMd,
                            ),
                            Text(
                              'De: ${debt.fromPlayerId.substring(0, debt.fromPlayerId.length > 6 ? 6 : debt.fromPlayerId.length)} ➔ Para: ${debt.toPlayerId.substring(0, debt.toPlayerId.length > 6 ? 6 : debt.toPlayerId.length)}',
                              style: AppTypography.bodySm.copyWith(color: AppColors.outline),
                            ),
                          ],
                        ),
                      ),
                      Text(
                        CurrencyFormatter.format(debt.amount),
                        style: AppTypography.labelMd.copyWith(color: AppColors.primary),
                      ),
                    ],
                  ),
                );
              },
            ),
    );
  }
}
