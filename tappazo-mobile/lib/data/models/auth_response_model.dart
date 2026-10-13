import '../../domain/entities/auth_session_entity.dart';
import '../../domain/entities/user_entity.dart';

class AuthResponseModel {
  final String token;
  final String userId;
  final String nickname;
  final String email;

  const AuthResponseModel({
    required this.token,
    required this.userId,
    required this.nickname,
    required this.email,
  });

  factory AuthResponseModel.fromJson(Map<String, dynamic> json) {
    return AuthResponseModel(
      token: json['token'] as String? ?? '',
      userId: json['userId'] as String? ?? '',
      nickname: json['nickname'] as String? ?? '',
      email: json['email'] as String? ?? '',
    );
  }

  Map<String, dynamic> toJson() {
    return {
      'token': token,
      'userId': userId,
      'nickname': nickname,
      'email': email,
    };
  }

  AuthSessionEntity toDomain() {
    return AuthSessionEntity(
      token: token,
      user: UserEntity(
        id: userId,
        nickname: nickname,
        email: email,
      ),
    );
  }
}

