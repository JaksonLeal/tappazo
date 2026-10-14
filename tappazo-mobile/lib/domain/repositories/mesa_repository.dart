import '../entities/debt_movement_entity.dart';
import '../entities/mesa_balance_entity.dart';
import '../entities/mesa_entity.dart';

abstract class MesaRepository {
  Future<MesaEntity> getMesa(String gameId);
  Future<MesaBalanceEntity> getMesaBalances(String gameId);
  Future<List<DebtMovementEntity>> getMesaDebts(String gameId);
}

