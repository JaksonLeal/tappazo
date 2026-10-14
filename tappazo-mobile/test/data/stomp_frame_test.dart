import 'package:flutter_test/flutter_test.dart';
import 'package:tappazo/data/datasources/stomp/stomp_frame.dart';

void main() {
  group('StompFrame', () {
    test('encode escapa headers salvo en CONNECT', () {
      final send = const StompFrame('SEND', headers: {'destination': '/app/a:b'}, body: '{}').encode();
      expect(send, 'SEND\ndestination:/app/a\\cb\n\n{}\u0000');

      final connect = const StompFrame('CONNECT', headers: {'Authorization': 'Bearer a:b'}).encode();
      expect(connect, contains('Authorization:Bearer a:b'));
    });

    test('parse de MESSAGE con CRLF y headers escapados', () {
      final frame = StompFrame.parse('MESSAGE\r\nsubscription:sub-0\r\ndestination:/topic/x\\cy\r\n\r\n{"a":1}');
      expect(frame.command, 'MESSAGE');
      expect(frame.headers['subscription'], 'sub-0');
      expect(frame.headers['destination'], '/topic/x:y');
      expect(frame.body, '{"a":1}');
    });
  });

  group('StompFrameDecoder', () {
    test('descarta heart-beats y une fragmentos', () {
      final decoder = StompFrameDecoder();
      var heartbeats = 0;
      expect(decoder.add('\n', onHeartbeat: () => heartbeats++), isEmpty);
      expect(decoder.add('MESSAGE\nsubscription:s\n\n{"x"', onHeartbeat: () => heartbeats++), isEmpty);
      final frames = decoder.add(':1}\u0000\nCONNECTED\nversion:1.2\n\n\u0000');
      expect(heartbeats, 1);
      expect(frames.map((f) => f.command), ['MESSAGE', 'CONNECTED']);
      expect(frames.first.body, '{"x":1}');
    });
  });
}
