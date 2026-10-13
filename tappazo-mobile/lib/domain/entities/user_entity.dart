class UserEntity {
  final String id;
  final String nickname;
  final String email;
  final String? profileImage;

  const UserEntity({
    required this.id,
    required this.nickname,
    required this.email,
    this.profileImage,
  });

  @override
  bool operator ==(Object other) =>
      identical(this, other) ||
      other is UserEntity &&
          runtimeType == other.runtimeType &&
          id == other.id &&
          nickname == other.nickname &&
          email == other.email;

  @override
  int get hashCode => id.hashCode ^ nickname.hashCode ^ email.hashCode;
}

