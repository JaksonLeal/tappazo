import '../../core/storage/secure_storage_service.dart';
import '../../domain/entities/user_entity.dart';
import '../models/auth_response_model.dart';
import '../models/user_model.dart';

abstract class AuthLocalDataSource {
  Future<void> saveAuthSession(AuthResponseModel authResponse);
  Future<UserEntity?> getSavedUser();
  Future<String?> getSavedToken();
  Future<bool> hasSavedToken();
  Future<void> clearAuthSession();
}

class AuthLocalDataSourceImpl implements AuthLocalDataSource {
  final SecureStorageService _storageService;

  AuthLocalDataSourceImpl(this._storageService);

  @override
  Future<void> saveAuthSession(AuthResponseModel authResponse) async {
    await _storageService.saveAuthData(
      token: authResponse.token,
      userId: authResponse.userId,
      email: authResponse.email,
      nickname: authResponse.nickname,
    );
  }

  @override
  Future<UserEntity?> getSavedUser() async {
    final userId = await _storageService.getUserId();
    final email = await _storageService.getEmail();
    final nickname = await _storageService.getNickname();

    if (userId == null || email == null || nickname == null) {
      return null;
    }

    return UserModel(
      id: userId,
      email: email,
      nickname: nickname,
    );
  }

  @override
  Future<String?> getSavedToken() async {
    return _storageService.getToken();
  }

  @override
  Future<bool> hasSavedToken() async {
    return _storageService.hasToken();
  }

  @override
  Future<void> clearAuthSession() async {
    await _storageService.clearAuthData();
  }
}

