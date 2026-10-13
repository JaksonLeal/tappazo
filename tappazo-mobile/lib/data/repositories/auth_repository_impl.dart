import 'package:google_sign_in/google_sign_in.dart';
import '../../domain/entities/auth_session_entity.dart';
import '../../domain/entities/user_entity.dart';
import '../../domain/repositories/auth_repository.dart';
import '../datasources/auth_local_datasource.dart';
import '../datasources/auth_remote_datasource.dart';

class AuthRepositoryImpl implements AuthRepository {
  final AuthRemoteDataSource _remoteDataSource;
  final AuthLocalDataSource _localDataSource;
  final GoogleSignIn _googleSignIn;

  AuthRepositoryImpl({
    required AuthRemoteDataSource remoteDataSource,
    required AuthLocalDataSource localDataSource,
    GoogleSignIn? googleSignIn,
  })  : _remoteDataSource = remoteDataSource,
        _localDataSource = localDataSource,
        _googleSignIn = googleSignIn ?? GoogleSignIn(scopes: ['email', 'profile']);

  @override
  Future<AuthSessionEntity> loginWithGoogle(String idToken) async {
    final responseModel = await _remoteDataSource.loginWithGoogle(idToken);
    await _localDataSource.saveAuthSession(responseModel);
    return responseModel.toDomain();
  }

  @override
  Future<AuthSessionEntity> loginWithLocal(String email, String nickname) async {
    final responseModel = await _remoteDataSource.loginWithLocal(email, nickname);
    await _localDataSource.saveAuthSession(responseModel);
    return responseModel.toDomain();
  }

  @override
  Future<UserEntity?> getCurrentUser() {
    return _localDataSource.getSavedUser();
  }

  @override
  Future<bool> isAuthenticated() {
    return _localDataSource.hasSavedToken();
  }

  @override
  Future<void> logout() async {
    try {
      if (await _googleSignIn.isSignedIn()) {
        await _googleSignIn.signOut();
      }
    } catch (_) {
      // Ignorar errores al cerrar sesión en el cliente de Google si no estaba logueado
    }
    await _localDataSource.clearAuthSession();
  }
}

