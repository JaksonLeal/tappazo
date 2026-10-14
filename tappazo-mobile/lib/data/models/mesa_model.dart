import '../../domain/entities/mesa_entity.dart';
import 'game_participant_model.dart';

class MesaModel extends MesaEntity {
  const MesaModel({
    required super.gameId,
    required super.code,
    required super.hostId,
    required super.state,
    required super.playerCount,
    required super.participants,
  });

  factory MesaModel.fromJson(Map<String, dynamic> json) {
    final participantsJson = json['participants'] as List<dynamic>? ?? [];

    return MesaModel(
      gameId: json['gameId'] as String? ?? '',
      code: json['code'] as String? ?? '',
      hostId: json['hostId'] as String? ?? '',
      state: json['state'] as String? ?? 'LOBBY',
      playerCount: (json['playerCount'] as num?)?.toInt() ?? 0,
      participants: participantsJson
          .map((p) => GameParticipantModel.fromJson(p as Map<String, dynamic>))
          .toList(),
    );
  }
}

