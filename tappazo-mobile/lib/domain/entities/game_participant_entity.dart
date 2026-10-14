class GameParticipantEntity {
  final String id;
  final String gameId;
  final String userId;
  final String role; // PLAYER | SPECTATOR
  final String participantState; // ACTIVE | STAND_BY | REMOVED
  final DateTime? joinedAt;
  final String? nickname;

  const GameParticipantEntity({
    required this.id,
    required this.gameId,
    required this.userId,
    required this.role,
    required this.participantState,
    this.joinedAt,
    this.nickname,
  });

  bool get isPlayer => role == 'PLAYER';
  bool get isSpectator => role == 'SPECTATOR';
  bool get isActive => participantState == 'ACTIVE';

  /// Nombre para mostrar: si viene nickname se usa, o los primeros caracteres del ID
  String get displayName =>
      (nickname != null && nickname!.isNotEmpty) ? nickname! : 'Jugador ${userId.substring(0, userId.length > 5 ? 5 : userId.length)}';
}

