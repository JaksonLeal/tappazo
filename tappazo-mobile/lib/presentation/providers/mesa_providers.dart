import 'package:flutter_riverpod/flutter_riverpod.dart';
import '../../data/datasources/mesa_remote_datasource.dart';
import '../../data/repositories/mesa_repository_impl.dart';
import '../../domain/repositories/mesa_repository.dart';
import '../../domain/usecases/get_mesa_balances_usecase.dart';
import '../../domain/usecases/get_mesa_debts_usecase.dart';
import '../../domain/usecases/get_mesa_usecase.dart';
import 'auth_providers.dart';
import 'mesa_notifier.dart';
import 'mesa_state.dart';

// ─── Data Source ─────────────────────────────────────────────────────────────

final mesaRemoteDataSourceProvider = Provider<MesaRemoteDataSource>((ref) {
  final apiClient = ref.watch(apiClientProvider);
  return MesaRemoteDataSourceImpl(apiClient);
});

// ─── Repositorio ─────────────────────────────────────────────────────────────

final mesaRepositoryProvider = Provider<MesaRepository>((ref) {
  final remote = ref.watch(mesaRemoteDataSourceProvider);
  return MesaRepositoryImpl(remote);
});

// ─── Casos de Uso ─────────────────────────────────────────────────────────────

final getMesaUseCaseProvider = Provider<GetMesaUseCase>((ref) {
  final repo = ref.watch(mesaRepositoryProvider);
  return GetMesaUseCase(repo);
});

final getMesaBalancesUseCaseProvider = Provider<GetMesaBalancesUseCase>((ref) {
  final repo = ref.watch(mesaRepositoryProvider);
  return GetMesaBalancesUseCase(repo);
});

final getMesaDebtsUseCaseProvider = Provider<GetMesaDebtsUseCase>((ref) {
  final repo = ref.watch(mesaRepositoryProvider);
  return GetMesaDebtsUseCase(repo);
});

// ─── Mesa Notifier Principal ─────────────────────────────────────────────────

final mesaNotifierProvider = StateNotifierProvider<MesaNotifier, MesaStateData>((ref) {
  return MesaNotifier(
    getMesaUseCase: ref.watch(getMesaUseCaseProvider),
    getMesaBalancesUseCase: ref.watch(getMesaBalancesUseCaseProvider),
    getMesaDebtsUseCase: ref.watch(getMesaDebtsUseCaseProvider),
  );
});

