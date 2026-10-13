import '../entities/auth_session_entity.dart';
import '../repositories/auth_repository.dart';

class LoginWithLocalUseCase {
  final AuthRepository _repository;

  LoginWithLocalUseCase(this._repository);

  Future<AuthSessionEntity> execute({
    required String email,
    required String nickname,
  }) {
    if (email.trim().isEmpty) {
      throw ArgumentError('El correo electrónico no puede estar vacío');
    }
    if (nickname.trim().isEmpty) {
      throw ArgumentError('El nickname no puede estar vacío');
    }
    return _repository.loginWithLocal(email.trim(), nickname.trim());
  }
}

