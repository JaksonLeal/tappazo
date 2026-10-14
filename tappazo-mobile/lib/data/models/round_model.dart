import '../../domain/entities/round_entity.dart';
import 'json_utils.dart';

class RoundModel extends RoundEntity {
  const RoundModel({
    required super.id,
    required super.gameId,
    required super.mode,
    required super.state,
    required super.drinkPrice,
    required super.roundType,
    super.startedAt,
    super.finishedAt,
    super.turnOrder,
    super.currentTurnPlayerId,
  });

  /// Parsea `RoundResponse`. Tolera campos opcionales futuros
  /// (`turnOrder`, `currentTurnPlayerId`) sin depender de ellos.
  factory RoundModel.fromJson(Map<String, dynamic> json) {
    return RoundModel(
      id: json['id'] as String? ?? '',
      gameId: json['gameId'] as String? ?? '',
      mode: json['mode'] as String? ?? '',
      state: RoundState.fromString(json['state'] as String?),
      drinkPrice: (json['drinkPrice'] as num?)?.toInt() ?? 0,
      roundType: json['roundType'] as String? ?? 'NORMAL',
      startedAt: parseInstant(json['startedAt']),
      finishedAt: parseInstant(json['finishedAt']),
      turnOrder: stringList(json['turnOrder']),
      currentTurnPlayerId: nonEmptyString(json['currentTurnPlayerId']),
    );
  }

  static RoundModel? tryParse(dynamic json) {
    if (json is Map<String, dynamic> && (json['id'] as String?)?.isNotEmpty == true) {
      return RoundModel.fromJson(json);
    }
    return null;
  }
}
