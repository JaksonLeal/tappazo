import 'dart:async';

import '../../core/constants/api_constants.dart';
import '../../domain/entities/game_decisions.dart';
import '../../domain/entities/game_event.dart';
import '../models/game_event_parser.dart';
import 'stomp/stomp_client.dart';

/// Fuente de datos en tiempo real sobre STOMP (Sección 68).
abstract class GameRealtimeDataSource {
  Stream<RealtimeConnectionStatus> get connectionStatus;
  RealtimeConnectionStatus get currentStatus;

  /// Errores de transporte/protocolo legibles para el usuario.
  Stream<String> get errors;

  Future<void> connect();
  Future<void> disconnect();

  /// Suscribe a `/topic/game/{gameId}` mientras haya oyentes.
  Stream<GameEvent> watchGame(String gameId);

  void submitAttempt({
    required String gameId,
    required String roundId,
    required int proposedNumber,
    String? photoReference,
    String? photoOmittedReason,
  });

  void submitTieBreakVote({
    required String gameId,
    required String tieBreakId,
    required TieBreakDecision decision,
  });

  void submitRevealVote({
    required String gameId,
    required String revealAttemptId,
    required RevealVoteDecision decision,
  });
}

class GameRealtimeDataSourceImpl implements GameRealtimeDataSource {
  final StompClient _client;

  GameRealtimeDataSourceImpl(this._client);

  static RealtimeConnectionStatus _map(StompConnectionStatus s) => switch (s) {
        StompConnectionStatus.disconnected => RealtimeConnectionStatus.disconnected,
        StompConnectionStatus.connecting => RealtimeConnectionStatus.connecting,
        StompConnectionStatus.connected => RealtimeConnectionStatus.connected,
        StompConnectionStatus.reconnecting => RealtimeConnectionStatus.reconnecting,
      };

  @override
  Stream<RealtimeConnectionStatus> get connectionStatus => _client.statusStream.map(_map);

  @override
  RealtimeConnectionStatus get currentStatus => _map(_client.status);

  @override
  Stream<String> get errors => _client.errors;

  @override
  Future<void> connect() => _client.connect();

  @override
  Future<void> disconnect() => _client.disconnect();

  @override
  Stream<GameEvent> watchGame(String gameId) {
    String? subscriptionId;
    late final StreamController<GameEvent> controller;
    controller = StreamController<GameEvent>(
      onListen: () {
        subscriptionId = _client.subscribe(ApiConstants.gameTopic(gameId), (frame) {
          final event = GameEventParser.parseBody(frame.body);
          if (event != null && !controller.isClosed) controller.add(event);
        });
      },
      onCancel: () {
        final id = subscriptionId;
        if (id != null) _client.unsubscribe(id);
        subscriptionId = null;
      },
    );
    return controller.stream;
  }

  @override
  void submitAttempt({
    required String gameId,
    required String roundId,
    required int proposedNumber,
    String? photoReference,
    String? photoOmittedReason,
  }) {
    _client.send(ApiConstants.attemptDestination(gameId), {
      'roundId': roundId,
      'proposedNumber': proposedNumber,
      'photoReference': photoReference,
      'photoOmittedReason': photoOmittedReason,
    });
  }

  @override
  void submitTieBreakVote({
    required String gameId,
    required String tieBreakId,
    required TieBreakDecision decision,
  }) {
    _client.send(ApiConstants.tieBreakVoteDestination(gameId), {
      'tieBreakId': tieBreakId,
      'decision': decision.apiValue,
    });
  }

  @override
  void submitRevealVote({
    required String gameId,
    required String revealAttemptId,
    required RevealVoteDecision decision,
  }) {
    if (!ApiConstants.revealVotingEnabled) {
      throw const StompException(
        'El backend aún no recibe votos de revelación (Sección 67).',
      );
    }
    _client.send(ApiConstants.revealVoteDestination(gameId), {
      'revealAttemptId': revealAttemptId,
      'decision': decision.apiValue,
    });
  }
}
