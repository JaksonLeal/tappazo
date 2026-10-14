import '../../domain/entities/game_participant_entity.dart';

class GameParticipantModel extends GameParticipantEntity {
  const GameParticipantModel({
    required super.id,
    required super.gameId,
    required super.userId,
    required super.role,
    required super.participantState,
    super.joinedAt,
    super.nickname,
  });

  factory GameParticipantModel.fromJson(Map<String, dynamic> json) {
    return GameParticipantModel(
      id: json['id'] as String? ?? '',
      gameId: json['gameId'] as String? ?? '',
      userId: json['userId'] as String? ?? '',
      role: json['role'] as String? ?? 'PLAYER',
      participantState: json['participantState'] as String? ?? 'ACTIVE',
      joinedAt: json['joinedAt'] != null ? DateTime.tryParse(json['joinedAt'] as String) : null,
      nickname: json['nickname'] as String?,
    );
  }

  Map<String, dynamic> toJson() {
    return {
      'id': id,
      'gameId': gameId,
      'userId': userId,
      'role': role,
      'participantState': participantState,
      'joinedAt': joinedAt?.toIso8601String(),
      if (nickname != null) 'nickname': nickname,
    };
  }
}

