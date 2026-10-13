import 'package:flutter_test/flutter_test.dart';
import 'package:tappazo/data/models/auth_response_model.dart';
import 'package:tappazo/data/models/user_model.dart';

void main() {
  group('AuthResponseModel tests', () {
    test('fromJson parses backend response correctly', () {
      final json = {
        'token': 'mock-jwt-token-xyz',
        'userId': 'user-uuid-123',
        'nickname': 'TapperoPro',
        'email': 'tappero@gmail.com',
      };

      final model = AuthResponseModel.fromJson(json);

      expect(model.token, equals('mock-jwt-token-xyz'));
      expect(model.userId, equals('user-uuid-123'));
      expect(model.nickname, equals('TapperoPro'));
      expect(model.email, equals('tappero@gmail.com'));

      final entity = model.toDomain();
      expect(entity.token, equals('mock-jwt-token-xyz'));
      expect(entity.user.id, equals('user-uuid-123'));
      expect(entity.user.nickname, equals('TapperoPro'));
      expect(entity.user.email, equals('tappero@gmail.com'));
    });
  });

  group('UserModel tests', () {
    test('fromJson and toJson roundtrip properly', () {
      final json = {
        'id': 'usr-001',
        'nickname': 'ElHost',
        'email': 'host@test.com',
        'profileImage': 'https://example.com/avatar.png',
      };

      final user = UserModel.fromJson(json);
      expect(user.id, equals('usr-001'));
      expect(user.nickname, equals('ElHost'));
      expect(user.email, equals('host@test.com'));
      expect(user.profileImage, equals('https://example.com/avatar.png'));

      final serialized = user.toJson();
      expect(serialized['id'], equals('usr-001'));
      expect(serialized['nickname'], equals('ElHost'));
    });
  });
}

