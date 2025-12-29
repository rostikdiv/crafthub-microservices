package com.crafthub.order_service.config;

import io.awspring.cloud.sqs.operations.SqsTemplate;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;
import software.amazon.awssdk.auth.credentials.DefaultCredentialsProvider;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.sqs.SqsAsyncClient;

@Configuration
@Profile("aws") // ❗️ Завантажувати тільки якщо активний профіль AWS
public class SqsConfig {

    // 1. Створюємо клієнт AWS SDK
    @Bean
    public SqsAsyncClient sqsAsyncClient() {
        return SqsAsyncClient.builder()
                .region(Region.EU_NORTH_1) // Твій регіон (зміни, якщо інший)
                .credentialsProvider(DefaultCredentialsProvider.create())
                .build();
    }

    // 2. Створюємо SqsTemplate (аналог KafkaTemplate)
    @Bean
    public SqsTemplate sqsTemplate(SqsAsyncClient sqsAsyncClient) {
        return SqsTemplate.builder()
                .sqsAsyncClient(sqsAsyncClient)
                .build();
    }
}