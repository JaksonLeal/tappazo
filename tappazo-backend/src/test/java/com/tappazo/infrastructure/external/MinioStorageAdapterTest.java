package com.tappazo.infrastructure.external;

import io.minio.*;
import io.minio.errors.ErrorResponseException;
import io.minio.http.Method;
import io.minio.messages.ErrorResponse;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import java.io.ByteArrayInputStream;
import java.nio.charset.StandardCharsets;
import java.time.Duration;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("MinioStorageAdapter Unit Tests")
class MinioStorageAdapterTest {

    @Mock
    private MinioClient minioClient;

    private MinioConfig minioConfig;
    private MinioStorageAdapter storageAdapter;

    @BeforeEach
    void setUp() {
        minioConfig = new MinioConfig();
        ReflectionTestUtils.setField(minioConfig, "endpoint", "http://localhost:9000");
        ReflectionTestUtils.setField(minioConfig, "accessKey", "minioadmin");
        ReflectionTestUtils.setField(minioConfig, "secretKey", "minioadmin");
        ReflectionTestUtils.setField(minioConfig, "defaultBucket", "tappazo-media");

        storageAdapter = new MinioStorageAdapter(minioClient, minioConfig);
    }

    @Test
    @DisplayName("uploadFile with stream successfully uploads object to bucket")
    void uploadFile_stream_success() throws Exception {
        when(minioClient.bucketExists(any(BucketExistsArgs.class))).thenReturn(true);
        when(minioClient.putObject(any(PutObjectArgs.class))).thenReturn(null);

        byte[] content = "test file content".getBytes(StandardCharsets.UTF_8);
        ByteArrayInputStream is = new ByteArrayInputStream(content);

        String result = storageAdapter.uploadFile("tappazo-media", "avatars/user-1.jpg", is, content.length, "image/jpeg");

        assertThat(result).isEqualTo("tappazo-media/avatars/user-1.jpg");
        verify(minioClient).putObject(any(PutObjectArgs.class));
    }

    @Test
    @DisplayName("uploadFile creates bucket if it does not exist")
    void uploadFile_createsBucket_ifMissing() throws Exception {
        when(minioClient.bucketExists(any(BucketExistsArgs.class))).thenReturn(false);
        doNothing().when(minioClient).makeBucket(any(MakeBucketArgs.class));
        when(minioClient.putObject(any(PutObjectArgs.class))).thenReturn(null);

        byte[] content = "hello".getBytes(StandardCharsets.UTF_8);
        ByteArrayInputStream is = new ByteArrayInputStream(content);

        String result = storageAdapter.uploadFile("reveals/round-1/p1.jpg", is, content.length, "image/jpeg");

        assertThat(result).isEqualTo("tappazo-media/reveals/round-1/p1.jpg");
        verify(minioClient).makeBucket(any(MakeBucketArgs.class));
        verify(minioClient).putObject(any(PutObjectArgs.class));
    }

    @Test
    @DisplayName("uploadFile byte array overload works as expected")
    void uploadFile_byteArray_success() throws Exception {
        when(minioClient.bucketExists(any(BucketExistsArgs.class))).thenReturn(true);
        when(minioClient.putObject(any(PutObjectArgs.class))).thenReturn(null);

        byte[] content = new byte[]{1, 2, 3};
        String result = storageAdapter.uploadFile("proofs/tx1.png", content, "image/png");

        assertThat(result).isEqualTo("tappazo-media/proofs/tx1.png");
        verify(minioClient).putObject(any(PutObjectArgs.class));
    }

    @Test
    @DisplayName("deleteFile successfully removes object from MinIO")
    void deleteFile_success() throws Exception {
        doNothing().when(minioClient).removeObject(any(RemoveObjectArgs.class));

        storageAdapter.deleteFile("tappazo-media", "avatars/user-1.jpg");

        verify(minioClient).removeObject(any(RemoveObjectArgs.class));
    }

    @Test
    @DisplayName("generatePresignedUploadUrl calls MinioClient with PUT method")
    void generatePresignedUploadUrl_success() throws Exception {
        when(minioClient.bucketExists(any(BucketExistsArgs.class))).thenReturn(true);
        when(minioClient.getPresignedObjectUrl(any(GetPresignedObjectUrlArgs.class)))
                .thenReturn("http://localhost:9000/tappazo-media/reveals/test.jpg?signature=xyz");

        String url = storageAdapter.generatePresignedUploadUrl("reveals/test.jpg", Duration.ofMinutes(15));

        assertThat(url).contains("signature=xyz");
        verify(minioClient).getPresignedObjectUrl(any(GetPresignedObjectUrlArgs.class));
    }

    @Test
    @DisplayName("generatePresignedDownloadUrl calls MinioClient with GET method")
    void generatePresignedDownloadUrl_success() throws Exception {
        when(minioClient.getPresignedObjectUrl(any(GetPresignedObjectUrlArgs.class)))
                .thenReturn("http://localhost:9000/tappazo-media/reveals/test.jpg?token=abc");

        String url = storageAdapter.generatePresignedDownloadUrl("reveals/test.jpg", Duration.ofHours(1));

        assertThat(url).contains("token=abc");
        verify(minioClient).getPresignedObjectUrl(any(GetPresignedObjectUrlArgs.class));
    }

    @Test
    @DisplayName("doesObjectExist returns true when object is found")
    void doesObjectExist_returnsTrue() throws Exception {
        when(minioClient.statObject(any(StatObjectArgs.class))).thenReturn(null);

        boolean exists = storageAdapter.doesObjectExist("tappazo-media", "test.jpg");

        assertThat(exists).isTrue();
    }

    @Test
    @DisplayName("doesObjectExist returns false when NoSuchKey is thrown")
    void doesObjectExist_returnsFalse_onNoSuchKey() throws Exception {
        ErrorResponse errorResponse = new ErrorResponse("NoSuchKey", "The specified key does not exist.", null, null, null, null, null);
        ErrorResponseException exception = new ErrorResponseException(errorResponse, null, null);

        when(minioClient.statObject(any(StatObjectArgs.class))).thenThrow(exception);

        boolean exists = storageAdapter.doesObjectExist("tappazo-media", "missing.jpg");

        assertThat(exists).isFalse();
    }

    @Test
    @DisplayName("uploadFile throws StorageException when MinIO client fails")
    void uploadFile_throwsStorageException_onFailure() throws Exception {
        when(minioClient.bucketExists(any(BucketExistsArgs.class))).thenReturn(true);
        when(minioClient.putObject(any(PutObjectArgs.class)))
                .thenThrow(new RuntimeException("Connection error"));

        byte[] content = "test".getBytes(StandardCharsets.UTF_8);
        ByteArrayInputStream is = new ByteArrayInputStream(content);

        assertThatThrownBy(() -> storageAdapter.uploadFile("error.jpg", is, content.length, "text/plain"))
                .isInstanceOf(StorageException.class)
                .hasMessageContaining("Error al subir archivo a MinIO");
    }
}
