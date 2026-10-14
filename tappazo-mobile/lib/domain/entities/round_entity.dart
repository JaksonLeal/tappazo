/// Estados de la ronda tal como los emite el backend (RoundState).
/// El cliente solo los refleja; nunca transiciona la ronda por su cuenta.
enum RoundState {
  starting('STARTING', 'Iniciando'),
  revealing('REVEALING', 'Revelando tapas'),
  validating('VALIDATING', 'Validando'),
  resolving('RESOLVING', 'Resolviendo'),
  tieBreak('TIE_BREAK', 'Desempate'),
  finished('FINISHED', 'Finalizada'),
  unknown('UNKNOWN', 'Desconocido');

  final String apiValue;
  final String displayName;

  const RoundState(this.apiValue, this.displayName);

  /// El backend acepta intentos de revelación solo en REVEALING / VALIDATING.
  bool get acceptsReveals => this == revealing || this == validating;

  static RoundState fromString(String? value) {
    for (final s in RoundState.values) {
      if (s.apiValue == value) return s;
    }
    return RoundState.unknown;
  }
}

class RoundEntity {
  final String id;
  final String gameId;
  final String mode;
  final RoundState state;
  final int drinkPrice;
  final String roundType; // NORMAL | TIE_BREAK_ROUND
  final DateTime? startedAt;
  final DateTime? finishedAt;

  /// Orden de revelación congelado (Sección 22). El backend actual aún no lo
  /// expone en RoundResponse; si llega, la UI lo usa para resaltar el turno.
  final List<String> turnOrder;

  /// Jugador al que le toca revelar, si el backend lo informa.
  final String? currentTurnPlayerId;

  const RoundEntity({
    required this.id,
    required this.gameId,
    required this.mode,
    required this.state,
    required this.drinkPrice,
    required this.roundType,
    this.startedAt,
    this.finishedAt,
    this.turnOrder = const [],
    this.currentTurnPlayerId,
  });

  bool get isTieBreakRound => roundType == 'TIE_BREAK_ROUND';
  bool get isFinished => state == RoundState.finished;

  /// `true` si el backend informa explícitamente el turno actual.
  bool get hasTurnInfo => currentTurnPlayerId != null;

  RoundEntity copyWith({RoundState? state}) => RoundEntity(
        id: id,
        gameId: gameId,
        mode: mode,
        state: state ?? this.state,
        drinkPrice: drinkPrice,
        roundType: roundType,
        startedAt: startedAt,
        finishedAt: finishedAt,
        turnOrder: turnOrder,
        currentTurnPlayerId: currentTurnPlayerId,
      );
}
