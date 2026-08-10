package com.milhub.payment_service.config;

import org.apache.kafka.clients.admin.NewTopic;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.kafka.config.TopicBuilder;

/**
 * Configuration for Kafka topics used by the Payment Service.
 */
@Configuration
public class KafkaConfig {

    /**
     * Declares the topic for successful payment events.
     */
    @Bean
    public NewTopic paymentSuccessTopic() {
        return TopicBuilder.name("payment-success-topic")
                .partitions(1)
                .replicas(1)
                .build();
    }
}