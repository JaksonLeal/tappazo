import '../../domain/entities/debt_movement_entity.dart';

class DebtMovementModel extends DebtMovementEntity {
  const DebtMovementModel({
    required super.id,
    required super.gameId,
    super.roundId,
    required super.fromPlayerId,
    required super.toPlayerId,
    required super.amount,
    required super.reason,
    super.createdAt,
  });

  factory DebtMovementModel.fromJson(Map<String, dynamic> json) {
    return DebtMovementModel(
      id: json['id'] as String? ?? '',
      gameId: json['gameId'] as String? ?? '',
      roundId: json['roundId'] as String?,
      fromPlayerId: json['fromPlayerId'] as String? ?? '',
      toPlayerId: json['toPlayerId'] as String? ?? '',
      amount: (json['amount'] as num?)?.toInt() ?? 0,
      reason: json['reason'] as String? ?? 'ROUND_LOSS',
      createdAt: json['createdAt'] != null ? DateTime.tryParse(json['createdAt'] as String) : null,
    );
  }
}

