package com.crafthub.notification_service.config;

import com.crafthub.notification_service.dto.OrderPlacedEventDTO;
import org.apache.kafka.common.serialization.StringDeserializer;
import org.springframework.boot.autoconfigure.kafka.KafkaProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;
import org.springframework.kafka.config.ConcurrentKafkaListenerContainerFactory;
import org.springframework.kafka.core.ConsumerFactory;
import org.springframework.kafka.core.DefaultKafkaConsumerFactory;
import org.springframework.kafka.support.serializer.JsonDeserializer;

@Configuration
@Profile("local")
public class KafkaConsumerConfig {

    /**
     * Ми використовуємо KafkaProperties (стандартний механізм Spring),
     * щоб зберегти всі твої робочі налаштування з'єднання (localhost:29092 тощо).
     */
    @Bean
    public ConsumerFactory<String, OrderPlacedEventDTO> consumerFactory(KafkaProperties kafkaProperties) {

        // 1. Налаштовуємо JSON десеріалізатор
        JsonDeserializer<OrderPlacedEventDTO> deserializer = new JsonDeserializer<>(OrderPlacedEventDTO.class);
        deserializer.setRemoveTypeHeaders(false);
        deserializer.addTrustedPackages("*"); // Довіряємо всім пакетам
        deserializer.setUseTypeMapperForKey(true);

        // 2. Створюємо фабрику, використовуючи властивості Spring Boot + наш десеріалізатор
        // kafkaProperties.buildConsumerProperties(null) - це те, що було в твоєму старому коді
        return new DefaultKafkaConsumerFactory<>(
                kafkaProperties.buildConsumerProperties(null),
                new StringDeserializer(),
                deserializer // ✅ Підставляємо JSON замість String
        );
    }

    @Bean
    public ConcurrentKafkaListenerContainerFactory<String, OrderPlacedEventDTO> kafkaListenerContainerFactory(
            ConsumerFactory<String, OrderPlacedEventDTO> consumerFactory) {

        ConcurrentKafkaListenerContainerFactory<String, OrderPlacedEventDTO> factory =
                new ConcurrentKafkaListenerContainerFactory<>();
        factory.setConsumerFactory(consumerFactory);

        // Залишаємо твоє налаштування Tracing
        factory.getContainerProperties().setObservationEnabled(true);

        return factory;
    }
}