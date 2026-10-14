/// Representa un movimiento bruto individual de deuda generado por una ronda (Sección 50).
class DebtMovementEntity {
  final String id;
  final String gameId;
  final String? roundId;
  final String fromPlayerId;
  final String toPlayerId;
  final int amount;
  final String reason;
  final DateTime? createdAt;

  const DebtMovementEntity({
    required this.id,
    required this.gameId,
    this.roundId,
    required this.fromPlayerId,
    required this.toPlayerId,
    required this.amount,
    required this.reason,
    this.createdAt,
  });

  String get reasonDisplay {
    switch (reason) {
      case 'ROUND_LOSS':
        return 'Pérdida de ronda';
      case 'TIE_BREAK':
        return 'Desempate';
      case 'TIE_BREAK_ROUND':
        return 'Ronda de desempate';
      case 'STAND_BY_REMOVAL':
        return 'Retiro en stand-by';
      case 'REVEAL_INVALID':
        return 'Revelación inválida';
      default:
        return reason;
    }
  }
}

