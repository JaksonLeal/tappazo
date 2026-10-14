import 'package:flutter_riverpod/flutter_riverpod.dart';
import '../../domain/usecases/get_mesa_balances_usecase.dart';
import '../../domain/usecases/get_mesa_debts_usecase.dart';
import '../../domain/usecases/get_mesa_usecase.dart';
import 'mesa_state.dart';

class MesaNotifier extends StateNotifier<MesaStateData> {
  final GetMesaUseCase _getMesaUseCase;
  final GetMesaBalancesUseCase _getMesaBalancesUseCase;
  final GetMesaDebtsUseCase _getMesaDebtsUseCase;

  MesaNotifier({
    required GetMesaUseCase getMesaUseCase,
    required GetMesaBalancesUseCase getMesaBalancesUseCase,
    required GetMesaDebtsUseCase getMesaDebtsUseCase,
  })  : _getMesaUseCase = getMesaUseCase,
        _getMesaBalancesUseCase = getMesaBalancesUseCase,
        _getMesaDebtsUseCase = getMesaDebtsUseCase,
        super(const MesaInitial());

  /// Carga la información completa de la mesa, sus balances netos y movimientos de deuda
  Future<void> loadMesaData(String gameId) async {
    state = const MesaLoading(message: 'Cargando datos de la mesa...');
    try {
      final mesaFuture = _getMesaUseCase.execute(gameId);
      final balanceFuture = _getMesaBalancesUseCase.execute(gameId);
      final debtsFuture = _getMesaDebtsUseCase.execute(gameId);

      final results = await Future.wait([mesaFuture, balanceFuture, debtsFuture]);

      state = MesaLoaded(
        mesa: results[0] as dynamic,
        balance: results[1] as dynamic,
        debts: results[2] as dynamic,
      );
    } catch (e) {
      state = MesaError('Error al cargar la mesa: $e');
    }
  }

  /// Recarga los datos de la mesa sin resetear la pantalla
  Future<void> refresh(String gameId) async {
    try {
      final mesa = await _getMesaUseCase.execute(gameId);
      final balance = await _getMesaBalancesUseCase.execute(gameId);
      final debts = await _getMesaDebtsUseCase.execute(gameId);

      state = MesaLoaded(mesa: mesa, balance: balance, debts: debts);
    } catch (e) {
      if (state is! MesaLoaded) {
        state = MesaError('Error al refrescar la mesa: $e');
      }
    }
  }
}

