import 'dart:async';

import 'package:flutter_test/flutter_test.dart';
import 'package:tappazo/domain/entities/voice_entities.dart';
import 'package:tappazo/domain/repositories/voice_repository.dart';
import 'package:tappazo/domain/services/voice_call_service.dart';
import 'package:tappazo/domain/usecases/get_voice_token_usecase.dart';
import 'package:tappazo/presentation/providers/voice_notifier.dart';

class FakeVoiceRepo implements VoiceRepository {
  int calls = 0;
  Object? failWith;
  @override
  Future<VoiceToken> fetchVoiceToken(String gameId) async {
    calls++;
    if (failWith != null) throw failWith!;
    return VoiceToken(token: 'tok-$calls', roomName: 'game-$gameId', serverUrl: 'ws://lk:7880');
  }
}

class FakeVoiceService implements VoiceCallService {
  final ctrl = StreamController<VoiceRoomSnapshot>.broadcast(sync: true);
  VoiceRoomSnapshot _current = const VoiceRoomSnapshot(status: VoiceConnectionStatus.idle);
  final connects = <String>[];
  bool micEnabled = false;
  bool? speaker;
  Object? micError;
  Object? connectError;

  void emit(VoiceRoomSnapshot s) {
    _current = s;
    ctrl.add(s);
  }

  @override
  Stream<VoiceRoomSnapshot> get snapshots => ctrl.stream;
  @override
  VoiceRoomSnapshot get current => _current;

  @override
  Future<void> connect({required String url, required String token}) async {
    connects.add(token);
    emit(const VoiceRoomSnapshot(status: VoiceConnectionStatus.connecting));
    if (connectError != null) {
      emit(const VoiceRoomSnapshot(
          status: VoiceConnectionStatus.disconnected, disconnectReason: VoiceDisconnectReason.network));
      throw connectError!;
    }
    emit(const VoiceRoomSnapshot(status: VoiceConnectionStatus.connected, participants: [
      VoiceParticipant(identity: 'me', isLocal: true),
      VoiceParticipant(identity: 'u2', isLocal: false, isSpeaking: true, isMuted: false),
    ]));
  }

  @override
  Future<void> setMicrophoneEnabled(bool enabled) async {
    if (micError != null) throw micError!;
    micEnabled = enabled;
    emit(VoiceRoomSnapshot(status: _current.status, micEnabled: enabled, participants: _current.participants));
  }

  @override
  Future<void> setSpeakerOn(bool on) async => speaker = on;

  @override
  Future<void> disconnect() async {
    emit(const VoiceRoomSnapshot(status: VoiceConnectionStatus.idle, disconnectReason: VoiceDisconnectReason.userLeft));
  }
}

void main() {
  late FakeVoiceRepo repo;
  late FakeVoiceService service;
  late StreamController<bool> network;
  late VoiceNotifier notifier;

  setUp(() {
    repo = FakeVoiceRepo();
    service = FakeVoiceService();
    network = StreamController<bool>.broadcast(sync: true);
    notifier = VoiceNotifier(
      gameId: 'g1',
      getToken: GetVoiceTokenUseCase(repo),
      service: service,
      networkChanges: network.stream,
      rejoinDelay: const Duration(milliseconds: 10),
    );
  });

  tearDown(() {
    if (notifier.mounted) notifier.dispose();
  });

  test('join: pide token al backend y entra a la sala con micrófono apagado', () async {
    await notifier.join();
    expect(repo.calls, 1);
    expect(service.connects, ['tok-1']);
    expect(notifier.state.status, VoiceConnectionStatus.connected);
    expect(notifier.state.micEnabled, isFalse);
    expect(notifier.state.participants, hasLength(2));
    expect(notifier.state.speakingCount, 1);
  });

  test('activar y desactivar el micrófono', () async {
    await notifier.join();
    await notifier.toggleMic();
    expect(service.micEnabled, isTrue);
    expect(notifier.state.micEnabled, isTrue);
    await notifier.toggleMic();
    expect(notifier.state.micEnabled, isFalse);
  });

  test('permiso de micrófono denegado muestra un mensaje claro', () async {
    await notifier.join();
    service.micError = Exception('NotAllowedError: Permission denied');
    await notifier.toggleMic();
    expect(notifier.state.micEnabled, isFalse);
    expect(notifier.state.error, contains('Permiso de micrófono'));
    expect(notifier.state.isTogglingMic, isFalse);
  });

  test('sin estar en la llamada no se puede activar el micrófono', () async {
    await notifier.toggleMic();
    expect(service.micEnabled, isFalse);
  });

  test('fallo al obtener token deja la voz desconectada con error', () async {
    repo.failWith = Exception('boom');
    await notifier.join();
    expect(notifier.state.status, VoiceConnectionStatus.disconnected);
    expect(notifier.state.error, isNotNull);
    expect(service.connects, isEmpty);
  });

  test('salir cuelga la llamada y no reintenta', () async {
    await notifier.join();
    await notifier.leave();
    expect(notifier.state.status, VoiceConnectionStatus.idle);
    await Future<void>.delayed(const Duration(milliseconds: 50));
    expect(repo.calls, 1);
  });

  test('caída de red de la sala: reintenta con un token nuevo', () async {
    await notifier.join();
    service.emit(const VoiceRoomSnapshot(
        status: VoiceConnectionStatus.disconnected, disconnectReason: VoiceDisconnectReason.network));
    expect(notifier.state.error, contains('Reintentando'));
    await Future<void>.delayed(const Duration(milliseconds: 50));
    expect(repo.calls, 2, reason: 'nuevo token al reconectar');
    expect(notifier.state.status, VoiceConnectionStatus.connected);
  });

  test('sin internet no reintenta; al volver la red se reincorpora', () async {
    await notifier.join();
    network.add(false);
    service.emit(const VoiceRoomSnapshot(
        status: VoiceConnectionStatus.disconnected, disconnectReason: VoiceDisconnectReason.network));
    await Future<void>.delayed(const Duration(milliseconds: 50));
    expect(repo.calls, 1);

    network.add(true);
    await Future<void>.delayed(const Duration(milliseconds: 20));
    expect(repo.calls, 2);
    expect(notifier.state.status, VoiceConnectionStatus.connected);
  });

  test('expulsión (identidad duplicada) no reintenta', () async {
    await notifier.join();
    service.emit(const VoiceRoomSnapshot(
        status: VoiceConnectionStatus.disconnected, disconnectReason: VoiceDisconnectReason.removed));
    await Future<void>.delayed(const Duration(milliseconds: 50));
    expect(repo.calls, 1);
    expect(notifier.state.error, contains('otro dispositivo'));
  });

  test('altavoz se alterna', () async {
    await notifier.join();
    await notifier.toggleSpeaker();
    expect(notifier.state.speakerOn, isFalse);
    expect(service.speaker, isFalse);
  });
}
