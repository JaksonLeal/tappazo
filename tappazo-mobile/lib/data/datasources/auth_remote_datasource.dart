import 'package:dio/dio.dart';
import '../../core/constants/api_constants.dart';
import '../../core/network/api_client.dart';
import '../models/auth_response_model.dart';

abstract class AuthRemoteDataSource {
  Future<AuthResponseModel> loginWithGoogle(String idToken);
  Future<AuthResponseModel> loginWithLocal(String email, String nickname);
}

class AuthRemoteDataSourceImpl implements AuthRemoteDataSource {
  final ApiClient _apiClient;

  AuthRemoteDataSourceImpl(this._apiClient);

  @override
  Future<AuthResponseModel> loginWithGoogle(String idToken) async {
    try {
      final response = await _apiClient.dio.post(
        ApiConstants.authGoogle,
        data: {'idToken': idToken},
      );

      if (response.statusCode == 200 && response.data != null) {
        return AuthResponseModel.fromJson(response.data as Map<String, dynamic>);
      }
      throw DioException(
        requestOptions: response.requestOptions,
        response: response,
        message: 'Respuesta inválida del servidor al autenticar con Google',
      );
    } on DioException {
      rethrow;
    } catch (e) {
      throw Exception('Error inesperado durante la autenticación con Google: $e');
    }
  }

  @override
  Future<AuthResponseModel> loginWithLocal(String email, String nickname) async {
    try {
      final response = await _apiClient.dio.post(
        ApiConstants.authLocal,
        data: {
          'email': email,
          'nickname': nickname,
        },
      );

      if (response.statusCode == 200 && response.data != null) {
        return AuthResponseModel.fromJson(response.data as Map<String, dynamic>);
      }
      throw DioException(
        requestOptions: response.requestOptions,
        response: response,
        message: 'Respuesta inválida del servidor al autenticar en modo local',
      );
    } on DioException {
      rethrow;
    } catch (e) {
      throw Exception('Error inesperado durante la autenticación local: $e');
    }
  }
}

