import 'package:flutter_secure_storage/flutter_secure_storage.dart';

class SecureStorageService {
  final FlutterSecureStorage _storage;

  SecureStorageService({FlutterSecureStorage? storage})
      : _storage = storage ??
            const FlutterSecureStorage(
              aOptions: AndroidOptions(
                encryptedSharedPreferences: true,
              ),
              iOptions: IOSOptions(
                accessibility: KeychainAccessibility.first_unlock,
              ),
            );

  static const String _keyToken = 'tappazo_jwt_token';
  static const String _keyUserId = 'tappazo_user_id';
  static const String _keyEmail = 'tappazo_user_email';
  static const String _keyNickname = 'tappazo_user_nickname';

  Future<void> saveAuthData({
    required String token,
    required String userId,
    required String email,
    required String nickname,
  }) async {
    await Future.wait([
      _storage.write(key: _keyToken, value: token),
      _storage.write(key: _keyUserId, value: userId),
      _storage.write(key: _keyEmail, value: email),
      _storage.write(key: _keyNickname, value: nickname),
    ]);
  }

  Future<String?> getToken() async {
    return _storage.read(key: _keyToken);
  }

  Future<String?> getUserId() async {
    return _storage.read(key: _keyUserId);
  }

  Future<String?> getEmail() async {
    return _storage.read(key: _keyEmail);
  }

  Future<String?> getNickname() async {
    return _storage.read(key: _keyNickname);
  }

  Future<bool> hasToken() async {
    final token = await getToken();
    return token != null && token.isNotEmpty;
  }

  Future<void> clearAuthData() async {
    await Future.wait([
      _storage.delete(key: _keyToken),
      _storage.delete(key: _keyUserId),
      _storage.delete(key: _keyEmail),
      _storage.delete(key: _keyNickname),
    ]);
  }
}

