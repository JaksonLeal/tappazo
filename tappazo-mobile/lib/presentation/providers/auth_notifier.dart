import 'package:flutter_riverpod/flutter_riverpod.dart';
import 'package:google_sign_in/google_sign_in.dart';
import '../../domain/usecases/check_auth_status_usecase.dart';
import '../../domain/usecases/login_with_google_usecase.dart';
import '../../domain/usecases/login_with_local_usecase.dart';
import '../../domain/usecases/logout_usecase.dart';
import 'auth_state.dart';

class AuthNotifier extends StateNotifier<AuthState> {
  final LoginWithGoogleUseCase _loginWithGoogleUseCase;
  final LoginWithLocalUseCase _loginWithLocalUseCase;
  final CheckAuthStatusUseCase _checkAuthStatusUseCase;
  final LogoutUseCase _logoutUseCase;
  final GoogleSignIn _googleSignIn;

  AuthNotifier({
    required LoginWithGoogleUseCase loginWithGoogleUseCase,
    required LoginWithLocalUseCase loginWithLocalUseCase,
    required CheckAuthStatusUseCase checkAuthStatusUseCase,
    required LogoutUseCase logoutUseCase,
    GoogleSignIn? googleSignIn,
  })  : _loginWithGoogleUseCase = loginWithGoogleUseCase,
        _loginWithLocalUseCase = loginWithLocalUseCase,
        _checkAuthStatusUseCase = checkAuthStatusUseCase,
        _logoutUseCase = logoutUseCase,
        _googleSignIn = googleSignIn ?? GoogleSignIn(scopes: ['email', 'profile']),
        super(const AuthInitial());

  /// Verifica si existe una sesión activa persistida en el dispositivo
  Future<void> checkAuthStatus() async {
    state = const AuthLoading(message: 'Verificando sesión...');
    try {
      final user = await _checkAuthStatusUseCase.execute();
      if (user != null) {
        state = Authenticated(user);
      } else {
        state = const Unauthenticated();
      }
    } catch (e) {
      state = const Unauthenticated();
    }
  }

  /// Inicia sesión con la cuenta de Google seleccionada
  Future<void> signInWithGoogle() async {
    state = const AuthLoading(message: 'Conectando con Google...');
    try {
      final googleAccount = await _googleSignIn.signIn();
      if (googleAccount == null) {
        // El usuario canceló la selección de cuenta en el diálogo de Google
        state = const Unauthenticated();
        return;
      }

      state = const AuthLoading(message: 'Validando token con backend Tappazo...');
      final googleAuth = await googleAccount.authentication;
      final idToken = googleAuth.idToken;

      if (idToken == null || idToken.isEmpty) {
        state = const AuthError('No se pudo obtener el ID Token de Google.');
        return;
      }

      final session = await _loginWithGoogleUseCase.execute(idToken);
      state = Authenticated(session.user);
    } catch (e) {
      state = AuthError('Error al iniciar sesión con Google: $e');
    }
  }

  /// Inicia sesión de desarrollo/pruebas locales sin requerir OAuth de Google
  Future<void> signInWithLocal({
    required String email,
    required String nickname,
  }) async {
    state = const AuthLoading(message: 'Autenticando en entorno de pruebas...');
    try {
      final session = await _loginWithLocalUseCase.execute(
        email: email,
        nickname: nickname,
      );
      state = Authenticated(session.user);
    } catch (e) {
      state = AuthError('Error en autenticación de pruebas: $e');
    }
  }

  /// Cierra la sesión activa
  Future<void> signOut() async {
    state = const AuthLoading(message: 'Cerrando sesión...');
    try {
      await _logoutUseCase.execute();
      state = const Unauthenticated();
    } catch (e) {
      state = AuthError('Error al cerrar sesión: $e');
    }
  }
}

