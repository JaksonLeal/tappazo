import 'package:dio/dio.dart';
import '../../core/constants/api_constants.dart';
import '../../core/network/api_client.dart';
import '../models/debt_movement_model.dart';
import '../models/mesa_balance_model.dart';
import '../models/mesa_model.dart';

abstract class MesaRemoteDataSource {
  Future<MesaModel> getMesa(String gameId);
  Future<MesaBalanceModel> getMesaBalances(String gameId);
  Future<List<DebtMovementModel>> getMesaDebts(String gameId);
}

class MesaRemoteDataSourceImpl implements MesaRemoteDataSource {
  final ApiClient _apiClient;

  MesaRemoteDataSourceImpl(this._apiClient);

  @override
  Future<MesaModel> getMesa(String gameId) async {
    try {
      final response = await _apiClient.dio.get('${ApiConstants.mesas}/$gameId');

      if (response.statusCode == 200 && response.data != null) {
        return MesaModel.fromJson(response.data as Map<String, dynamic>);
      }
      throw DioException(
        requestOptions: response.requestOptions,
        response: response,
        message: 'Respuesta inesperada al consultar la mesa',
      );
    } on DioException {
      rethrow;
    } catch (e) {
      throw Exception('Error al consultar estado de la mesa: $e');
    }
  }

  @override
  Future<MesaBalanceModel> getMesaBalances(String gameId) async {
    try {
      final response = await _apiClient.dio.get('${ApiConstants.mesas}/$gameId/balances');

      if (response.statusCode == 200 && response.data != null) {
        return MesaBalanceModel.fromJson(response.data as Map<String, dynamic>);
      }
      throw DioException(
        requestOptions: response.requestOptions,
        response: response,
        message: 'Respuesta inesperada al consultar balances de la mesa',
      );
    } on DioException {
      rethrow;
    } catch (e) {
      throw Exception('Error al consultar balances de la mesa: $e');
    }
  }

  @override
  Future<List<DebtMovementModel>> getMesaDebts(String gameId) async {
    try {
      final response = await _apiClient.dio.get('${ApiConstants.mesas}/$gameId/debts');

      if (response.statusCode == 200 && response.data != null) {
        final list = response.data as List<dynamic>;
        return list.map((d) => DebtMovementModel.fromJson(d as Map<String, dynamic>)).toList();
      }
      return [];
    } on DioException {
      rethrow;
    } catch (e) {
      throw Exception('Error al consultar deudas de la mesa: $e');
    }
  }
}

