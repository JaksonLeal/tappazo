import '../../domain/entities/debt_movement_entity.dart';
import '../../domain/entities/mesa_balance_entity.dart';
import '../../domain/entities/mesa_entity.dart';
import '../../domain/repositories/mesa_repository.dart';
import '../datasources/mesa_remote_datasource.dart';

class MesaRepositoryImpl implements MesaRepository {
  final MesaRemoteDataSource _remoteDataSource;

  MesaRepositoryImpl(this._remoteDataSource);

  @override
  Future<MesaEntity> getMesa(String gameId) {
    return _remoteDataSource.getMesa(gameId);
  }

  @override
  Future<MesaBalanceEntity> getMesaBalances(String gameId) {
    return _remoteDataSource.getMesaBalances(gameId);
  }

  @override
  Future<List<DebtMovementEntity>> getMesaDebts(String gameId) {
    return _remoteDataSource.getMesaDebts(gameId);
  }
}

