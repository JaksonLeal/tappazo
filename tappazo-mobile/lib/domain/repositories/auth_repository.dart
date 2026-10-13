import '../entities/auth_session_entity.dart';
import '../entities/user_entity.dart';

abstract class AuthRepository {
  /// Inicia sesión con el token ID obtenido de Google
  Future<AuthSessionEntity> loginWithGoogle(String idToken);

  /// Inicia sesión simulada/local para desarrollo
  Future<AuthSessionEntity> loginWithLocal(String email, String nickname);

  /// Obtiene los datos del usuario logueado en la sesión activa
  Future<UserEntity?> getCurrentUser();

  /// Comprueba si existe un token válido persistido
  Future<bool> isAuthenticated();

  /// Cierra la sesión activa y borra credenciales seguras
  Future<void> logout();
}

