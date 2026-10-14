import 'game_entity.dart';
import 'game_participant_entity.dart';

class GameDetailsEntity {
  final GameEntity game;
  final List<GameParticipantEntity> participants;
  final dynamic currentRound;

  const GameDetailsEntity({
    required this.game,
    required this.participants,
    this.currentRound,
  });

  /// Participantes ordenados alfabéticamente por nickname / displayName (Sección 4 y 9 del spec)
  List<GameParticipantEntity> get participantsSortedAlphabetically {
    final list = List<GameParticipantEntity>.from(participants);
    list.sort((a, b) => a.displayName.toLowerCase().compareTo(b.displayName.toLowerCase()));
    return list;
  }
}

