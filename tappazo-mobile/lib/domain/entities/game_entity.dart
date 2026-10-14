import 'game_mode.dart';
import 'game_state.dart';

class GameEntity {
  final String id;
  final String code;
  final String hostId;
  final GameState state;
  final int maxPlayers;
  final int currentDrinkPrice;
  final GameMode currentMode;
  final DateTime? createdAt;

  const GameEntity({
    required this.id,
    required this.code,
    required this.hostId,
    required this.state,
    required this.maxPlayers,
    required this.currentDrinkPrice,
    required this.currentMode,
    this.createdAt,
  });

  bool isHost(String userId) => hostId == userId;
  bool get isLobby => state == GameState.lobby;
  bool get isInProgress => state == GameState.inProgress;
  bool get isFinished => state == GameState.finished;
}

