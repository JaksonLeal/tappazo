package com.tappazo.infrastructure.external;

import io.minio.MinioClient;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * Configuración del cliente MinIO (compatible con Amazon S3 / Cloudflare R2).
 * Sección 26 del documento maestro.
 */
@Configuration
public class MinioConfig {

    @Value("${tappazo.minio.endpoint:http://localhost:9000}")
    private String endpoint;

    @Value("${tappazo.minio.access-key:minioadmin}")
    private String accessKey;

    @Value("${tappazo.minio.secret-key:minioadmin}")
    private String secretKey;

    @Value("${tappazo.minio.default-bucket:tappazo-media}")
    private String defaultBucket;

    @Bean
    public MinioClient minioClient() {
        return MinioClient.builder()
                .endpoint(endpoint)
                .credentials(accessKey, secretKey)
                .build();
    }

    public String getEndpoint() {
        return endpoint;
    }

    public String getAccessKey() {
        return accessKey;
    }

    public String getSecretKey() {
        return secretKey;
    }

    public String getDefaultBucket() {
        return defaultBucket;
    }
}
