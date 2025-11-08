package com.crafthub.notification_service.config;

import org.springframework.boot.autoconfigure.kafka.KafkaProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;
import org.springframework.kafka.config.ConcurrentKafkaListenerContainerFactory;
import org.springframework.kafka.core.ConsumerFactory;
import org.springframework.kafka.core.DefaultKafkaConsumerFactory;

@Configuration
@Profile("kafka")
public class KafkaConsumerConfig {

    /**
     * Цей бін перевизначає стандартну "фабрику" слухачів Kafka.
     * Ми робимо це, щоб примусово увімкнути "Observation" (трасування).
     */
    @Bean
    public ConcurrentKafkaListenerContainerFactory<String, String> kafkaListenerContainerFactory(
            KafkaProperties kafkaProperties) {

        // 1. Створюємо стандартну ConsumerFactory
        ConsumerFactory<String, String> consumerFactory =
                new DefaultKafkaConsumerFactory<>(kafkaProperties.buildConsumerProperties(null));

        // 2. Створюємо "фабрику" контейнерів
        ConcurrentKafkaListenerContainerFactory<String, String> factory =
                new ConcurrentKafkaListenerContainerFactory<>();
        factory.setConsumerFactory(consumerFactory);

        // 3. ❗️ ЦЕ І Є ВИРІШЕННЯ:
        // Ми примусово вмикаємо Micrometer Tracing для всіх @KafkaListener
        factory.getContainerProperties().setObservationEnabled(true);

        return factory;
    }
}