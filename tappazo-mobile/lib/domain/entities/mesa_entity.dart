import 'game_participant_entity.dart';

/// Resumen general de la mesa (/mesas/{gameId})
class MesaEntity {
  final String gameId;
  final String code;
  final String hostId;
  final String state;
  final int playerCount;
  final List<GameParticipantEntity> participants;

  const MesaEntity({
    required this.gameId,
    required this.code,
    required this.hostId,
    required this.state,
    required this.playerCount,
    required this.participants,
  });
}

