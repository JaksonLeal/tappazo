import 'dart:async';

import 'package:connectivity_plus/connectivity_plus.dart';

/// Observa si el dispositivo tiene una red disponible.
///
/// Nota: "online" significa que hay una interfaz de red activa (wifi, datos,
/// ethernet...). No garantiza que el backend sea alcanzable; para eso está la
/// reconexión STOMP y el refresco REST.
abstract class NetworkMonitor {
  /// Emite `true`/`false` solo cuando cambia la disponibilidad de red.
  Stream<bool> get onlineChanges;

  Future<bool> isOnline();

  /// Estado actual seguido de los cambios posteriores.
  Stream<bool> watchOnline() async* {
    yield await isOnline();
    yield* onlineChanges;
  }
}

class ConnectivityNetworkMonitor extends NetworkMonitor {
  final Connectivity _connectivity;

  ConnectivityNetworkMonitor([Connectivity? connectivity]) : _connectivity = connectivity ?? Connectivity();

  static bool _hasNetwork(List<ConnectivityResult> results) =>
      results.any((r) => r != ConnectivityResult.none);

  @override
  Stream<bool> get onlineChanges => _connectivity.onConnectivityChanged.map(_hasNetwork).distinct();

  @override
  Future<bool> isOnline() async {
    try {
      return _hasNetwork(await _connectivity.checkConnectivity());
    } catch (_) {
      // Si el plugin falla, asumimos red disponible y dejamos que STOMP decida.
      return true;
    }
  }
}
