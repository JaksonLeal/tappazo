import '../entities/user_entity.dart';
import '../repositories/auth_repository.dart';

class CheckAuthStatusUseCase {
  final AuthRepository _repository;

  CheckAuthStatusUseCase(this._repository);

  Future<UserEntity?> execute() async {
    final isAuth = await _repository.isAuthenticated();
    if (!isAuth) {
      return null;
    }
    return _repository.getCurrentUser();
  }
}

