/// Frame STOMP 1.2 (https://stomp.github.io/stomp-specification-1.2.html).
///
/// Implementación mínima y sin dependencias: Spring usa STOMP sobre WebSocket
/// en `/ws` (Sección 68) y solo necesitamos CONNECT, SUBSCRIBE, UNSUBSCRIBE,
/// SEND, DISCONNECT y los frames de servidor CONNECTED, MESSAGE, ERROR.
class StompFrame {
  final String command;
  final Map<String, String> headers;
  final String body;

  const StompFrame(this.command, {this.headers = const {}, this.body = ''});

  static const String _nul = '\u0000';

  /// Serializa el frame. CONNECT/CONNECTED no escapan headers (spec 1.2).
  String encode() {
    final escape = command != 'CONNECT' && command != 'CONNECTED';
    final buffer = StringBuffer()..write('$command\n');
    headers.forEach((key, value) {
      buffer
        ..write(escape ? _escape(key) : key)
        ..write(':')
        ..write(escape ? _escape(value) : value)
        ..write('\n');
    });
    buffer
      ..write('\n')
      ..write(body)
      ..write(_nul);
    return buffer.toString();
  }

  static String _escape(String value) => value
      .replaceAll('\\', '\\\\')
      .replaceAll('\r', '\\r')
      .replaceAll('\n', '\\n')
      .replaceAll(':', '\\c');

  static String _unescape(String value) {
    final out = StringBuffer();
    for (var i = 0; i < value.length; i++) {
      final ch = value[i];
      if (ch == '\\' && i + 1 < value.length) {
        final next = value[++i];
        switch (next) {
          case 'n':
            out.write('\n');
          case 'r':
            out.write('\r');
          case 'c':
            out.write(':');
          case '\\':
            out.write('\\');
          default:
            out.write(next);
        }
      } else {
        out.write(ch);
      }
    }
    return out.toString();
  }

  /// Parsea un frame crudo (sin el NUL terminal).
  static StompFrame parse(String raw) {
    final normalized = raw.replaceAll('\r\n', '\n');
    final separator = normalized.indexOf('\n\n');
    final head = separator >= 0 ? normalized.substring(0, separator) : normalized;
    final body = separator >= 0 ? normalized.substring(separator + 2) : '';

    final lines = head.split('\n');
    final command = lines.first.trim();
    final unescape = command != 'CONNECTED';
    final headers = <String, String>{};
    for (final line in lines.skip(1)) {
      final idx = line.indexOf(':');
      if (idx <= 0) continue;
      final key = unescape ? _unescape(line.substring(0, idx)) : line.substring(0, idx);
      final value = unescape ? _unescape(line.substring(idx + 1)) : line.substring(idx + 1);
      // Spec 1.2: si un header se repite, solo cuenta la primera aparición.
      headers.putIfAbsent(key, () => value);
    }
    return StompFrame(command, headers: headers, body: body);
  }

  @override
  String toString() => 'StompFrame($command, $headers, ${body.length} chars)';
}

/// Acumula fragmentos del socket y emite frames completos.
/// Los saltos de línea sueltos entre frames son heart-beats y se descartan.
class StompFrameDecoder {
  String _pending = '';

  /// Devuelve los frames completos contenidos en [chunk] (y bytes previos).
  /// [onHeartbeat] se invoca si el fragmento contenía al menos un heart-beat.
  List<StompFrame> add(String chunk, {void Function()? onHeartbeat}) {
    _pending += chunk;
    final frames = <StompFrame>[];
    while (true) {
      var start = 0;
      while (start < _pending.length && (_pending[start] == '\n' || _pending[start] == '\r')) {
        start++;
      }
      if (start > 0) {
        onHeartbeat?.call();
        _pending = _pending.substring(start);
      }
      final nul = _pending.indexOf(StompFrame._nul);
      if (nul < 0) break;
      final raw = _pending.substring(0, nul);
      _pending = _pending.substring(nul + 1);
      if (raw.trim().isNotEmpty) {
        frames.add(StompFrame.parse(raw));
      }
    }
    return frames;
  }

  void reset() => _pending = '';
}
