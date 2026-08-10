package com.milhub.order_service.config;

import io.awspring.cloud.sqs.operations.SqsTemplate;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;
import software.amazon.awssdk.auth.credentials.DefaultCredentialsProvider;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.sqs.SqsAsyncClient;

/**
 * Amazon SQS configuration.
 * Only loaded when the 'aws' profile is active.
 */
@Configuration
@Profile("aws")
public class SqsConfig {

    /**
     * Configures the AWS SDK SQS asynchronous client.
     */
    @Bean
    public SqsAsyncClient sqsAsyncClient() {
        return SqsAsyncClient.builder()
                .region(Region.EU_NORTH_1) // Adjust region as needed
                .credentialsProvider(DefaultCredentialsProvider.create())
                .build();
    }

    /**
     * Configures the SqsTemplate for high-level SQS operations.
     */
    @Bean
    public SqsTemplate sqsTemplate(SqsAsyncClient sqsAsyncClient) {
        return SqsTemplate.builder()
                .sqsAsyncClient(sqsAsyncClient)
                .build();
    }
}