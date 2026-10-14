import 'debt_movement_entity.dart';
import 'game_decisions.dart';

/// Resultado de una ronda resuelta por el backend (evento ROUND_RESOLVED).
/// El cliente solo lo presenta: nunca calcula perdedores (Sección 69).
class RoundResolutionEntity {
  final String roundId;
  final List<String> confirmedLoserIds;
  final List<String> winnerIds;
  final bool hasTie;
  final List<DebtMovementEntity> debtMovements;

  const RoundResolutionEntity({
    required this.roundId,
    required this.confirmedLoserIds,
    required this.winnerIds,
    required this.hasTie,
    this.debtMovements = const [],
  });
}

enum TieBreakStatus {
  pending('PENDING'),
  resolved('RESOLVED');

  final String apiValue;
  const TieBreakStatus(this.apiValue);

  static TieBreakStatus fromString(String? value) =>
      value == 'RESOLVED' ? TieBreakStatus.resolved : TieBreakStatus.pending;
}

/// Desempate en curso (Sección 33). Se va completando con los eventos
/// TIE_BREAK_STARTED → TIE_BREAK_DECIDER_SELECTED → TIE_BREAK_RESOLVED.
class TieBreakEntity {
  final String id;
  final String? sourceRoundId;
  final String? tieType;
  final int affectedLoserSlots;
  final TieBreakStatus status;
  final bool allTiedCase;

  /// Jugadores empatados, si el backend los informa (hoy no se envían).
  final List<String> tiedPlayerIds;
  final String? selectedDeciderId;
  final TieBreakDecision? resolutionMode;
  final String? tieBreakRoundId;

  const TieBreakEntity({
    required this.id,
    this.sourceRoundId,
    this.tieType,
    this.affectedLoserSlots = 0,
    this.status = TieBreakStatus.pending,
    this.allTiedCase = false,
    this.tiedPlayerIds = const [],
    this.selectedDeciderId,
    this.resolutionMode,
    this.tieBreakRoundId,
  });

  bool get isResolved => status == TieBreakStatus.resolved;
  bool get hasDecider => selectedDeciderId != null && selectedDeciderId!.isNotEmpty;

  TieBreakEntity copyWith({
    TieBreakStatus? status,
    String? selectedDeciderId,
    TieBreakDecision? resolutionMode,
    String? tieBreakRoundId,
  }) {
    return TieBreakEntity(
      id: id,
      sourceRoundId: sourceRoundId,
      tieType: tieType,
      affectedLoserSlots: affectedLoserSlots,
      status: status ?? this.status,
      allTiedCase: allTiedCase,
      tiedPlayerIds: tiedPlayerIds,
      selectedDeciderId: selectedDeciderId ?? this.selectedDeciderId,
      resolutionMode: resolutionMode ?? this.resolutionMode,
      tieBreakRoundId: tieBreakRoundId ?? this.tieBreakRoundId,
    );
  }
}
