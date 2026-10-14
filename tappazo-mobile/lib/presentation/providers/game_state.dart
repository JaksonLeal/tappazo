import '../../domain/entities/game_details_entity.dart';
import '../../domain/entities/game_entity.dart';

sealed class GameStateData {
  const GameStateData();
}

class GameInitial extends GameStateData {
  const GameInitial();
}

class GameLoading extends GameStateData {
  final String? message;
  const GameLoading({this.message});
}

class GameLoaded extends GameStateData {
  final GameDetailsEntity details;
  final bool isUpdatingSettings;

  const GameLoaded({
    required this.details,
    this.isUpdatingSettings = false,
  });

  GameEntity get game => details.game;

  GameLoaded copyWith({
    GameDetailsEntity? details,
    bool? isUpdatingSettings,
  }) {
    return GameLoaded(
      details: details ?? this.details,
      isUpdatingSettings: isUpdatingSettings ?? this.isUpdatingSettings,
    );
  }
}

class GameError extends GameStateData {
  final String message;
  const GameError(this.message);
}

