/// Representa una deuda neta consolidada entre dos jugadores dentro de una partida (Sección 51 y 52).
class NetDebtEntity {
  final String fromPlayerId;
  final String toPlayerId;
  final int amount;

  const NetDebtEntity({
    required this.fromPlayerId,
    required this.toPlayerId,
    required this.amount,
  });

  String get fromPlayerDisplay =>
      'Jugador ${fromPlayerId.substring(0, fromPlayerId.length > 5 ? 5 : fromPlayerId.length)}';

  String get toPlayerDisplay =>
      'Jugador ${toPlayerId.substring(0, toPlayerId.length > 5 ? 5 : toPlayerId.length)}';
}

