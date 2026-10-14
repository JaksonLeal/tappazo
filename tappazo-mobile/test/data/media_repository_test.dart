import 'package:dio/dio.dart';
import 'package:flutter_test/flutter_test.dart';
import 'package:tappazo/core/network/error_messages.dart';
import 'package:tappazo/data/datasources/media_remote_datasource.dart';
import 'package:tappazo/data/repositories/media_repository_impl.dart';
import 'package:tappazo/domain/repositories/media_repository.dart';

DioException dioError(DioExceptionType type, {int? status}) {
  final options = RequestOptions(path: '/x');
  return DioException(
    requestOptions: options,
    type: type,
    response: status != null ? Response(requestOptions: options, statusCode: status) : null,
  );
}

class FlakyMediaDataSource implements MediaRemoteDataSource {
  int presignCalls = 0;
  int putCalls = 0;
  int failPutTimes = 0;
  DioExceptionType failType = DioExceptionType.connectionError;
  final puts = <String>[];

  @override
  Future<PresignedUpload> requestUpload({
    required String fileName,
    required String contentType,
    required String folder,
  }) async {
    presignCalls++;
    expect(folder, 'reveals');
    expect(contentType, 'image/jpeg');
    return const PresignedUpload(
      uploadUrl: 'http://minio:9000/tappazo/reveals/abc.jpg?X-Amz-Signature=sig',
      fileKey: 'reveals/abc.jpg',
      downloadUrl: 'http://minio:9000/get/abc.jpg',
    );
  }

  @override
  Future<void> uploadBytes({required String uploadUrl, required List<int> bytes, required String contentType}) async {
    putCalls++;
    if (putCalls <= failPutTimes) throw dioError(failType);
    puts.add(uploadUrl);
  }

  @override
  Future<String> getDownloadUrl(String fileKey) async => 'http://dl/$fileKey';
}

void main() {
  group('MediaRepositoryImpl (Sección 26)', () {
    test('presign → PUT al bucket → devuelve fileKey como photoReference', () async {
      final ds = FlakyMediaDataSource();
      final repo = MediaRepositoryImpl(ds, retryDelay: Duration.zero);
      final key = await repo.uploadRevealPhoto([1, 2, 3]);
      expect(key, 'reveals/abc.jpg');
      expect(ds.presignCalls, 1);
      expect(ds.puts.single, contains('X-Amz-Signature'));
      // La URL de descarga devuelta en el presign queda en caché.
      expect(await repo.resolvePhotoUrl(key), 'http://minio:9000/get/abc.jpg');
    });

    test('reintenta una vez ante un fallo transitorio de red', () async {
      final ds = FlakyMediaDataSource()..failPutTimes = 1;
      final repo = MediaRepositoryImpl(ds, retryDelay: Duration.zero);
      expect(await repo.uploadRevealPhoto([1]), 'reveals/abc.jpg');
      expect(ds.putCalls, 2);
    });

    test('no reintenta errores definitivos (403 de firma inválida)', () async {
      final ds = FlakyMediaDataSource()
        ..failPutTimes = 5
        ..failType = DioExceptionType.badResponse;
      final repo = MediaRepositoryImpl(ds, retryDelay: Duration.zero);
      await expectLater(repo.uploadRevealPhoto([1]), throwsA(isA<DioException>()));
      expect(ds.putCalls, 1);
    });
  });

  group('describeError', () {
    test('mensajes legibles para errores de red', () {
      expect(describeError(dioError(DioExceptionType.connectionError)), contains('conexión'));
      expect(describeError(dioError(DioExceptionType.receiveTimeout)), contains('tardó'));
      expect(describeError(dioError(DioExceptionType.badResponse, status: 401)), contains('sesión'));
      expect(describeError(dioError(DioExceptionType.badResponse, status: 503)), contains('servidor'));
      expect(describeError(Exception('x'), fallback: 'F'), 'F');
    });

    test('isTransientNetworkError', () {
      expect(isTransientNetworkError(dioError(DioExceptionType.connectionError)), isTrue);
      expect(isTransientNetworkError(dioError(DioExceptionType.badResponse, status: 502)), isTrue);
      expect(isTransientNetworkError(dioError(DioExceptionType.badResponse, status: 403)), isFalse);
      expect(isTransientNetworkError(Exception('x')), isFalse);
    });
  });
}
