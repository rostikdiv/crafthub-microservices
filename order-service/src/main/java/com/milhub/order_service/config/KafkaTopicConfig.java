package com.milhub.order_service.config;

import org.apache.kafka.clients.admin.NewTopic;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.kafka.config.TopicBuilder;

@Configuration
public class KafkaTopicConfig {

    @Bean
    public NewTopic orderPlacedTopic() {
        return TopicBuilder.name("order-placed-topic")
                .partitions(1)
                .replicas(1)
                .build();
    }

    @Bean
    public NewTopic returnEventsTopic() {
        return TopicBuilder.name("return-events")
                .partitions(1)
                .replicas(1)
                .build();
    }

    @Bean
    public NewTopic orderFailedEventsTopic() {
        return TopicBuilder.name("order-failed-events")
                .partitions(1)
                .replicas(1)
                .build();
    }
}
