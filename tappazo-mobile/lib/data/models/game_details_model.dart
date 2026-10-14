import '../../domain/entities/game_details_entity.dart';
import 'game_model.dart';
import 'game_participant_model.dart';
import 'round_model.dart';

class GameDetailsModel extends GameDetailsEntity {
  const GameDetailsModel({
    required super.game,
    required super.participants,
    super.currentRound,
  });

  factory GameDetailsModel.fromJson(Map<String, dynamic> json) {
    final gameJson = json['game'] as Map<String, dynamic>? ?? {};
    final participantsJson = json['participants'] as List<dynamic>? ?? [];

    return GameDetailsModel(
      game: GameModel.fromJson(gameJson),
      participants: participantsJson
          .map((p) => GameParticipantModel.fromJson(p as Map<String, dynamic>))
          .toList(),
      currentRound: RoundModel.tryParse(json['currentRound']),
    );
  }
}
