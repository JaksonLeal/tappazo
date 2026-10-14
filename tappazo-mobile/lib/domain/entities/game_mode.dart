/// Modalidades de juego de Tappazo (Secciones 28 y 67 del spec).
enum GameMode {
  ultimoPierde('ULTIMO_PIERDE', 'Último Pierde', 2),
  pequenosPagan('PEQUENOS_PAGAN', 'Los pequeños pagan', 3),
  ultimosDosPierden('ULTIMOS_DOS_PIERDEN', 'Últimos dos pierden', 4);

  final String apiValue;
  final String displayName;
  final int minPlayers;

  const GameMode(this.apiValue, this.displayName, this.minPlayers);

  static GameMode fromString(String value) {
    for (final mode in GameMode.values) {
      if (mode.apiValue == value || mode.name == value) {
        return mode;
      }
    }
    return GameMode.ultimoPierde;
  }
}

