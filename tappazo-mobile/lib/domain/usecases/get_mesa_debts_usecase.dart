import '../entities/debt_movement_entity.dart';
import '../repositories/mesa_repository.dart';

class GetMesaDebtsUseCase {
  final MesaRepository _repository;

  GetMesaDebtsUseCase(this._repository);

  Future<List<DebtMovementEntity>> execute(String gameId) {
    if (gameId.trim().isEmpty) {
      throw ArgumentError('El ID de la partida no puede estar vacío');
    }
    return _repository.getMesaDebts(gameId.trim());
  }
}

