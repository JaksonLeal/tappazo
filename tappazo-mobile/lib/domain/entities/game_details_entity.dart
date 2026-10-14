import 'game_entity.dart';
import 'game_participant_entity.dart';
import 'round_entity.dart';

class GameDetailsEntity {
  final GameEntity game;
  final List<GameParticipantEntity> participants;
  final RoundEntity? currentRound;

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

  GameParticipantEntity? participantFor(String userId) {
    for (final p in participants) {
      if (p.userId == userId) return p;
    }
    return null;
  }

  /// Nombre visible de un jugador por su userId.
  String displayNameFor(String userId) =>
      participantFor(userId)?.displayName ??
      'Jugador ${userId.substring(0, userId.length > 5 ? 5 : userId.length)}';
}
