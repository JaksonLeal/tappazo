import 'package:dio/dio.dart';
import '../../core/constants/api_constants.dart';
import '../../core/network/api_client.dart';
import '../../domain/entities/game_mode.dart';
import '../models/game_details_model.dart';
import '../models/game_model.dart';
import '../models/game_participant_model.dart';

abstract class GameRemoteDataSource {
  Future<GameModel> createGame({
    required int drinkPrice,
    required GameMode initialMode,
    int? maxPlayers,
  });

  Future<GameParticipantModel> joinGame(String gameCode);

  Future<GameDetailsModel> getGameDetails(String id);

  Future<GameModel> getGameByCode(String code);

  Future<List<GameParticipantModel>> getGameParticipants(String id);

  Future<GameModel> updateGameSettings({
    required String id,
    int? drinkPrice,
    GameMode? mode,
  });
}

class GameRemoteDataSourceImpl implements GameRemoteDataSource {
  final ApiClient _apiClient;

  GameRemoteDataSourceImpl(this._apiClient);

  @override
  Future<GameModel> createGame({
    required int drinkPrice,
    required GameMode initialMode,
    int? maxPlayers,
  }) async {
    try {
      final data = <String, dynamic>{
        'drinkPrice': drinkPrice,
        'initialMode': initialMode.apiValue,
        if (maxPlayers != null) 'maxPlayers': maxPlayers,
      };

      final response = await _apiClient.dio.post(
        ApiConstants.games,
        data: data,
      );

      if (response.statusCode == 201 && response.data != null) {
        return GameModel.fromJson(response.data as Map<String, dynamic>);
      }
      throw DioException(
        requestOptions: response.requestOptions,
        response: response,
        message: 'Respuesta inesperada al crear la partida',
      );
    } on DioException {
      rethrow;
    } catch (e) {
      throw Exception('Error al crear la partida: $e');
    }
  }

  @override
  Future<GameParticipantModel> joinGame(String gameCode) async {
    try {
      final response = await _apiClient.dio.post(
        ApiConstants.joinGame,
        data: {'gameCode': gameCode.trim().toUpperCase()},
      );

      if (response.statusCode == 200 && response.data != null) {
        return GameParticipantModel.fromJson(response.data as Map<String, dynamic>);
      }
      throw DioException(
        requestOptions: response.requestOptions,
        response: response,
        message: 'Respuesta inesperada al unirse a la partida',
      );
    } on DioException {
      rethrow;
    } catch (e) {
      throw Exception('Error al unirse a la partida: $e');
    }
  }

  @override
  Future<GameDetailsModel> getGameDetails(String id) async {
    try {
      final response = await _apiClient.dio.get('${ApiConstants.games}/$id');

      if (response.statusCode == 200 && response.data != null) {
        return GameDetailsModel.fromJson(response.data as Map<String, dynamic>);
      }
      throw DioException(
        requestOptions: response.requestOptions,
        response: response,
        message: 'Respuesta inesperada al obtener detalles de la partida',
      );
    } on DioException {
      rethrow;
    } catch (e) {
      throw Exception('Error al obtener detalles de la partida: $e');
    }
  }

  @override
  Future<GameModel> getGameByCode(String code) async {
    try {
      final response = await _apiClient.dio.get('${ApiConstants.games}/code/${code.trim().toUpperCase()}');

      if (response.statusCode == 200 && response.data != null) {
        return GameModel.fromJson(response.data as Map<String, dynamic>);
      }
      throw DioException(
        requestOptions: response.requestOptions,
        response: response,
        message: 'Partida no encontrada con el código proporcionado',
      );
    } on DioException {
      rethrow;
    } catch (e) {
      throw Exception('Error al buscar partida por código: $e');
    }
  }

  @override
  Future<List<GameParticipantModel>> getGameParticipants(String id) async {
    try {
      final response = await _apiClient.dio.get('${ApiConstants.games}/$id/participants');

      if (response.statusCode == 200 && response.data != null) {
        final list = response.data as List<dynamic>;
        return list.map((p) => GameParticipantModel.fromJson(p as Map<String, dynamic>)).toList();
      }
      return [];
    } on DioException {
      rethrow;
    } catch (e) {
      throw Exception('Error al obtener participantes: $e');
    }
  }

  @override
  Future<GameModel> updateGameSettings({
    required String id,
    int? drinkPrice,
    GameMode? mode,
  }) async {
    try {
      final data = <String, dynamic>{
        if (drinkPrice != null) 'drinkPrice': drinkPrice,
        if (mode != null) 'mode': mode.apiValue,
      };

      final response = await _apiClient.dio.patch(
        '${ApiConstants.games}/$id/settings',
        data: data,
      );

      if (response.statusCode == 200 && response.data != null) {
        return GameModel.fromJson(response.data as Map<String, dynamic>);
      }
      throw DioException(
        requestOptions: response.requestOptions,
        response: response,
        message: 'Error al actualizar configuración de la partida',
      );
    } on DioException {
      rethrow;
    } catch (e) {
      throw Exception('Error al actualizar configuración de la partida: $e');
    }
  }
}

