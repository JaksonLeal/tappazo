import '../../domain/entities/net_debt_entity.dart';

class NetDebtModel extends NetDebtEntity {
  const NetDebtModel({
    required super.fromPlayerId,
    required super.toPlayerId,
    required super.amount,
  });

  factory NetDebtModel.fromJson(Map<String, dynamic> json) {
    return NetDebtModel(
      fromPlayerId: json['fromPlayerId'] as String? ?? '',
      toPlayerId: json['toPlayerId'] as String? ?? '',
      amount: (json['amount'] as num?)?.toInt() ?? 0,
    );
  }

  Map<String, dynamic> toJson() {
    return {
      'fromPlayerId': fromPlayerId,
      'toPlayerId': toPlayerId,
      'amount': amount,
    };
  }
}

