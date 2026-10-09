package com.tappazo.infrastructure.external;

import com.tappazo.application.port.out.FileStoragePort;
import io.minio.*;
import io.minio.errors.ErrorResponseException;
import io.minio.http.Method;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.io.ByteArrayInputStream;
import java.io.InputStream;
import java.time.Duration;
import java.util.Objects;
import java.util.concurrent.TimeUnit;

/**
 * Adaptador de almacenamiento compatible con S3 respaldado por MinIO.
 * Soporta operaciones de carga, eliminación y generación de URLs firmadas (PUT/GET).
 */
@Component
public class MinioStorageAdapter implements FileStoragePort {

    private static final Logger log = LoggerFactory.getLogger(MinioStorageAdapter.class);

    private final MinioClient minioClient;
    private final MinioConfig minioConfig;

    public MinioStorageAdapter(MinioClient minioClient, MinioConfig minioConfig) {
        this.minioClient = Objects.requireNonNull(minioClient);
        this.minioConfig = Objects.requireNonNull(minioConfig);
    }

    @Override
    public String uploadFile(String bucketName, String objectName, InputStream inputStream, long size, String contentType) {
        try {
            ensureBucketExists(bucketName);
            minioClient.putObject(
                    PutObjectArgs.builder()
                            .bucket(bucketName)
                            .object(objectName)
                            .stream(inputStream, size, -1)
                            .contentType(contentType != null ? contentType : "application/octet-stream")
                            .build()
            );
            log.info("Archivo subido con éxito: bucket={}, objectName={}", bucketName, objectName);
            return bucketName + "/" + objectName;
        } catch (Exception e) {
            log.error("Error al subir archivo a MinIO [bucket={}, objectName={}]: {}", bucketName, objectName, e.getMessage());
            throw new StorageException("Error al subir archivo a MinIO: " + objectName, e);
        }
    }

    @Override
    public String uploadFile(String objectName, InputStream inputStream, long size, String contentType) {
        return uploadFile(getDefaultBucket(), objectName, inputStream, size, contentType);
    }

    public String uploadFile(String objectName, byte[] data, String contentType) {
        return uploadFile(getDefaultBucket(), objectName, new ByteArrayInputStream(data), data.length, contentType);
    }

    @Override
    public void deleteFile(String bucketName, String objectName) {
        try {
            minioClient.removeObject(
                    RemoveObjectArgs.builder()
                            .bucket(bucketName)
                            .object(objectName)
                            .build()
            );
            log.info("Archivo eliminado con éxito: bucket={}, objectName={}", bucketName, objectName);
        } catch (Exception e) {
            log.error("Error al eliminar archivo de MinIO [bucket={}, objectName={}]: {}", bucketName, objectName, e.getMessage());
            throw new StorageException("Error al eliminar archivo de MinIO: " + objectName, e);
        }
    }

    @Override
    public void deleteFile(String objectName) {
        deleteFile(getDefaultBucket(), objectName);
    }

    @Override
    public String generatePresignedUploadUrl(String bucketName, String objectName, Duration expiry) {
        try {
            ensureBucketExists(bucketName);
            int expirySeconds = calculateExpirySeconds(expiry);
            return minioClient.getPresignedObjectUrl(
                    GetPresignedObjectUrlArgs.builder()
                            .method(Method.PUT)
                            .bucket(bucketName)
                            .object(objectName)
                            .expiry(expirySeconds, TimeUnit.SECONDS)
                            .build()
            );
        } catch (Exception e) {
            log.error("Error al generar URL prefirmada de subida [bucket={}, objectName={}]: {}", bucketName, objectName, e.getMessage());
            throw new StorageException("Error al generar URL prefirmada de subida: " + objectName, e);
        }
    }

    @Override
    public String generatePresignedUploadUrl(String objectName, Duration expiry) {
        return generatePresignedUploadUrl(getDefaultBucket(), objectName, expiry);
    }

    @Override
    public String generatePresignedDownloadUrl(String bucketName, String objectName, Duration expiry) {
        try {
            int expirySeconds = calculateExpirySeconds(expiry);
            return minioClient.getPresignedObjectUrl(
                    GetPresignedObjectUrlArgs.builder()
                            .method(Method.GET)
                            .bucket(bucketName)
                            .object(objectName)
                            .expiry(expirySeconds, TimeUnit.SECONDS)
                            .build()
            );
        } catch (Exception e) {
            log.error("Error al generar URL prefirmada de descarga [bucket={}, objectName={}]: {}", bucketName, objectName, e.getMessage());
            throw new StorageException("Error al generar URL prefirmada de descarga: " + objectName, e);
        }
    }

    @Override
    public String generatePresignedDownloadUrl(String objectName, Duration expiry) {
        return generatePresignedDownloadUrl(getDefaultBucket(), objectName, expiry);
    }

    @Override
    public boolean doesObjectExist(String bucketName, String objectName) {
        try {
            minioClient.statObject(
                    StatObjectArgs.builder()
                            .bucket(bucketName)
                            .object(objectName)
                            .build()
            );
            return true;
        } catch (ErrorResponseException e) {
            String code = e.errorResponse().code();
            if ("NoSuchKey".equals(code) || "NoSuchBucket".equals(code)) {
                return false;
            }
            throw new StorageException("Error al comprobar existencia de objeto en MinIO: " + objectName, e);
        } catch (Exception e) {
            throw new StorageException("Error al comprobar existencia de objeto en MinIO: " + objectName, e);
        }
    }

    @Override
    public void ensureBucketExists(String bucketName) {
        try {
            boolean found = minioClient.bucketExists(
                    BucketExistsArgs.builder()
                            .bucket(bucketName)
                            .build()
            );
            if (!found) {
                minioClient.makeBucket(
                        MakeBucketArgs.builder()
                                .bucket(bucketName)
                                .build()
                );
                log.info("Bucket creado exitosamente en MinIO: {}", bucketName);
            }
        } catch (Exception e) {
            log.warn("No se pudo verificar/crear el bucket '{}' en MinIO: {}", bucketName, e.getMessage());
            // No bloqueamos para permitir resiliencia durante inicialización / pruebas locales
        }
    }

    @Override
    public String getDefaultBucket() {
        return minioConfig.getDefaultBucket();
    }

    private int calculateExpirySeconds(Duration duration) {
        if (duration == null || duration.isNegative() || duration.isZero()) {
            return 3600; // 1 hora por defecto
        }
        long seconds = duration.toSeconds();
        // Límite de S3/MinIO: máx 7 días (604800 segundos)
        return (int) Math.min(seconds, 604800);
    }
}
