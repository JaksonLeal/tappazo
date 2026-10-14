/// Estados del ciclo de vida de una partida en Tappazo (Sección 11, 56).
enum GameState {
  lobby('LOBBY', 'En Lobby'),
  inProgress('IN_PROGRESS', 'En Curso'),
  finished('FINISHED', 'Finalizada');

  final String apiValue;
  final String displayName;

  const GameState(this.apiValue, this.displayName);

  static GameState fromString(String value) {
    for (final state in GameState.values) {
      if (state.apiValue == value || state.name == value) {
        return state;
      }
    }
    return GameState.lobby;
  }
}

