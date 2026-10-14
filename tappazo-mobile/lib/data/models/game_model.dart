import '../../domain/entities/game_entity.dart';
import '../../domain/entities/game_mode.dart';
import '../../domain/entities/game_state.dart';

class GameModel extends GameEntity {
  const GameModel({
    required super.id,
    required super.code,
    required super.hostId,
    required super.state,
    required super.maxPlayers,
    required super.currentDrinkPrice,
    required super.currentMode,
    super.createdAt,
  });

  factory GameModel.fromJson(Map<String, dynamic> json) {
    return GameModel(
      id: json['id'] as String? ?? '',
      code: json['code'] as String? ?? '',
      hostId: json['hostId'] as String? ?? '',
      state: GameState.fromString(json['state'] as String? ?? 'LOBBY'),
      maxPlayers: (json['maxPlayers'] as num?)?.toInt() ?? 10,
      currentDrinkPrice: (json['currentDrinkPrice'] as num?)?.toInt() ?? 0,
      currentMode: GameMode.fromString(json['currentMode'] as String? ?? 'ULTIMO_PIERDE'),
      createdAt: json['createdAt'] != null ? DateTime.tryParse(json['createdAt'] as String) : null,
    );
  }

  Map<String, dynamic> toJson() {
    return {
      'id': id,
      'code': code,
      'hostId': hostId,
      'state': state.apiValue,
      'maxPlayers': maxPlayers,
      'currentDrinkPrice': currentDrinkPrice,
      'currentMode': currentMode.apiValue,
      'createdAt': createdAt?.toIso8601String(),
    };
  }
}

