import '../entities/mesa_entity.dart';
import '../repositories/mesa_repository.dart';

class GetMesaUseCase {
  final MesaRepository _repository;

  GetMesaUseCase(this._repository);

  Future<MesaEntity> execute(String gameId) {
    if (gameId.trim().isEmpty) {
      throw ArgumentError('El ID de la partida no puede estar vacío');
    }
    return _repository.getMesa(gameId.trim());
  }
}

