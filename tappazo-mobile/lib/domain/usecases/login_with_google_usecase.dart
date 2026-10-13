import '../entities/auth_session_entity.dart';
import '../repositories/auth_repository.dart';

class LoginWithGoogleUseCase {
  final AuthRepository _repository;

  LoginWithGoogleUseCase(this._repository);

  Future<AuthSessionEntity> execute(String idToken) {
    if (idToken.trim().isEmpty) {
      throw ArgumentError('El idToken de Google no puede estar vacío');
    }
    return _repository.loginWithGoogle(idToken);
  }
}

