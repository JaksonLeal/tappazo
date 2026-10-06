package com.tappazo.application.port.out;

import java.io.InputStream;
import java.time.Duration;

/**
 * Puerto de salida para el almacenamiento de archivos (S3/MinIO)
 * según las especificaciones de la Sección 26.
 */
public interface FileStoragePort {

    /**
     * Sube un archivo con un stream de entrada en el bucket especificado.
     */
    String uploadFile(String bucketName, String objectName, InputStream inputStream, long size, String contentType);

    /**
     * Sube un archivo con un stream de entrada en el bucket por defecto.
     */
    String uploadFile(String objectName, InputStream inputStream, long size, String contentType);

    /**
     * Elimina un archivo del bucket especificado.
     */
    void deleteFile(String bucketName, String objectName);

    /**
     * Elimina un archivo del bucket por defecto.
     */
    void deleteFile(String objectName);

    /**
     * Genera una URL prefirmada para subida (HTTP PUT) en el bucket especificado.
     */
    String generatePresignedUploadUrl(String bucketName, String objectName, Duration expiry);

    /**
     * Genera una URL prefirmada para subida (HTTP PUT) en el bucket por defecto.
     */
    String generatePresignedUploadUrl(String objectName, Duration expiry);

    /**
     * Genera una URL prefirmada para descarga/lectura (HTTP GET) en el bucket especificado.
     */
    String generatePresignedDownloadUrl(String bucketName, String objectName, Duration expiry);

    /**
     * Genera una URL prefirmada para descarga/lectura (HTTP GET) en el bucket por defecto.
     */
    String generatePresignedDownloadUrl(String objectName, Duration expiry);

    /**
     * Verifica si un objeto existe en el bucket especificado.
     */
    boolean doesObjectExist(String bucketName, String objectName);

    /**
     * Asegura que el bucket existe, creándolo si es necesario.
     */
    void ensureBucketExists(String bucketName);

    /**
     * Retorna el nombre del bucket configurado por defecto.
     */
    String getDefaultBucket();
}
