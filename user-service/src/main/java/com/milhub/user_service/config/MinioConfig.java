package com.milhub.user_service.config;

import io.minio.MinioClient;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;

/**
 * Configuration class for initializing the MinIO client.
 */
@Configuration
@Profile("!cloud")
public class MinioConfig {

    @Value("${minio.url}")
    private String url;

    @Value("${minio.access-key}")
    private String accessKey;

    @Value("${minio.secret-key}")
    private String secretKey;

    @Value("${minio.external-url:http://localhost:9000}")
    private String externalUrl;

    @Value("${minio.region:#{null}}")
    private String region;

    /**
     * Initializes the MinioClient bean with the provided credentials and endpoint.
     */
    @Bean
    public MinioClient minioClient() {
        var builder = MinioClient.builder()
                .endpoint(url)
                .credentials(accessKey, secretKey);

        if (region != null && !region.isBlank()) {
            builder.region(region);
        }

        return builder.build();
    }
}