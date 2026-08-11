package com.milhub.user_service.config;

import org.apache.kafka.clients.admin.NewTopic;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.kafka.config.TopicBuilder;

@Configuration
public class KafkaTopicConfig {

    @Bean
    public NewTopic userVerificationTopic() {
        return TopicBuilder.name("user-verification-topic")
                .partitions(1)
                .replicas(1)
                .build();
    }
}
