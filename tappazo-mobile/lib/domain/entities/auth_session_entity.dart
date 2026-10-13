import 'user_entity.dart';

class AuthSessionEntity {
  final String token;
  final UserEntity user;

  const AuthSessionEntity({
    required this.token,
    required this.user,
  });
}

