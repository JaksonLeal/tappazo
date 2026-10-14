import '../entities/mesa_balance_entity.dart';
import '../repositories/mesa_repository.dart';

class GetMesaBalancesUseCase {
  final MesaRepository _repository;

  GetMesaBalancesUseCase(this._repository);

  Future<MesaBalanceEntity> execute(String gameId) {
    if (gameId.trim().isEmpty) {
      throw ArgumentError('El ID de la partida no puede estar vacío');
    }
    return _repository.getMesaBalances(gameId.trim());
  }
}

