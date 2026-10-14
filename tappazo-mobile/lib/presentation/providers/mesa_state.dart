import '../../domain/entities/debt_movement_entity.dart';
import '../../domain/entities/mesa_balance_entity.dart';
import '../../domain/entities/mesa_entity.dart';

sealed class MesaStateData {
  const MesaStateData();
}

class MesaInitial extends MesaStateData {
  const MesaInitial();
}

class MesaLoading extends MesaStateData {
  final String? message;
  const MesaLoading({this.message});
}

class MesaLoaded extends MesaStateData {
  final MesaEntity mesa;
  final MesaBalanceEntity balance;
  final List<DebtMovementEntity> debts;

  const MesaLoaded({
    required this.mesa,
    required this.balance,
    required this.debts,
  });

  MesaLoaded copyWith({
    MesaEntity? mesa,
    MesaBalanceEntity? balance,
    List<DebtMovementEntity>? debts,
  }) {
    return MesaLoaded(
      mesa: mesa ?? this.mesa,
      balance: balance ?? this.balance,
      debts: debts ?? this.debts,
    );
  }
}

class MesaError extends MesaStateData {
  final String message;
  const MesaError(this.message);
}

