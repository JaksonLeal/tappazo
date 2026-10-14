import 'dart:async';

import 'package:dio/dio.dart';

import '../../core/constants/api_constants.dart';
import '../../core/network/api_client.dart';
import '../../domain/repositories/media_repository.dart';

abstract class MediaRemoteDataSource {
  /// POST /api/v1/media/presigned-upload
  Future<PresignedUpload> requestUpload({
    required String fileName,
    required String contentType,
    required String folder,
  });

  /// PUT directo al bucket con la URL prefirmada (sin JWT: rompería la firma).
  Future<void> uploadBytes({
    required String uploadUrl,
    required List<int> bytes,
    required String contentType,
  });

  /// GET /api/v1/media/presigned-download?fileKey=...
  Future<String> getDownloadUrl(String fileKey);
}

class MediaRemoteDataSourceImpl implements MediaRemoteDataSource {
  final ApiClient _apiClient;
  final Dio _uploadDio;

  MediaRemoteDataSourceImpl(this._apiClient, {Dio? uploadDio})
      : _uploadDio = uploadDio ??
            Dio(BaseOptions(
              connectTimeout: ApiConstants.connectTimeout,
              sendTimeout: const Duration(seconds: 60),
              receiveTimeout: const Duration(seconds: 30),
            ));

  @override
  Future<PresignedUpload> requestUpload({
    required String fileName,
    required String contentType,
    required String folder,
  }) async {
    final response = await _apiClient.dio.post(
      ApiConstants.mediaUpload,
      data: {'fileName': fileName, 'contentType': contentType, 'folder': folder},
    );
    final data = response.data;
    if (response.statusCode == 200 && data is Map<String, dynamic>) {
      final uploadUrl = data['uploadUrl'] as String?;
      final fileKey = data['fileKey'] as String?;
      if (uploadUrl != null && fileKey != null) {
        return PresignedUpload(
          uploadUrl: uploadUrl,
          fileKey: fileKey,
          downloadUrl: data['downloadUrl'] as String?,
        );
      }
    }
    throw DioException(
      requestOptions: response.requestOptions,
      response: response,
      message: 'Respuesta inesperada al solicitar URL de subida',
    );
  }

  @override
  Future<void> uploadBytes({
    required String uploadUrl,
    required List<int> bytes,
    required String contentType,
  }) async {
    await _uploadDio.put<void>(
      uploadUrl,
      data: Stream<List<int>>.value(bytes),
      options: Options(headers: {
        Headers.contentTypeHeader: contentType,
        Headers.contentLengthHeader: bytes.length,
      }),
    );
  }

  @override
  Future<String> getDownloadUrl(String fileKey) async {
    // El backend devuelve la URL como String plano: evitamos el decode JSON.
    final response = await _apiClient.dio.get<String>(
      ApiConstants.mediaDownload,
      queryParameters: {'fileKey': fileKey},
      options: Options(responseType: ResponseType.plain),
    );
    var url = (response.data ?? '').trim();
    if (url.length >= 2 && url.startsWith('"') && url.endsWith('"')) {
      url = url.substring(1, url.length - 1);
    }
    if (url.isEmpty) {
      throw Exception('URL de descarga vacía para $fileKey');
    }
    return url;
  }
}
