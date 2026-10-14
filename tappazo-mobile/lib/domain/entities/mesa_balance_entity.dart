import 'net_debt_entity.dart';

/// Balances consolidados de la mesa actual (/mesas/{gameId}/balances)
class MesaBalanceEntity {
  final String gameId;
  final List<NetDebtEntity> netDebts;
  final int totalActiveDebt;

  const MesaBalanceEntity({
    required this.gameId,
    required this.netDebts,
    required this.totalActiveDebt,
  });
}

