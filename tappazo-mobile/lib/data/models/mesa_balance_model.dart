import '../../domain/entities/mesa_balance_entity.dart';
import 'net_debt_model.dart';

class MesaBalanceModel extends MesaBalanceEntity {
  const MesaBalanceModel({
    required super.gameId,
    required super.netDebts,
    required super.totalActiveDebt,
  });

  factory MesaBalanceModel.fromJson(Map<String, dynamic> json) {
    final netDebtsJson = json['netDebts'] as List<dynamic>? ?? [];

    return MesaBalanceModel(
      gameId: json['gameId'] as String? ?? '',
      netDebts: netDebtsJson
          .map((n) => NetDebtModel.fromJson(n as Map<String, dynamic>))
          .toList(),
      totalActiveDebt: (json['totalActiveDebt'] as num?)?.toInt() ?? 0,
    );
  }
}

