package com.crafthub.cart_service.config;

import com.crafthub.cart_service.dto.OrderPlacedEventDTO;
import org.apache.kafka.clients.consumer.ConsumerConfig;
import org.apache.kafka.common.serialization.StringDeserializer;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.kafka.config.ConcurrentKafkaListenerContainerFactory;
import org.springframework.kafka.core.ConsumerFactory;
import org.springframework.kafka.core.DefaultKafkaConsumerFactory;
import org.springframework.kafka.support.serializer.JsonDeserializer;

import java.util.HashMap;
import java.util.Map;

@Configuration
public class KafkaConsumerConfig {

    @Value("${spring.kafka.bootstrap-servers}") // Шукає spring -> kafka -> bootstrap-servers
    private String bootstrapServers;

    @Value("${spring.kafka.consumer.group-id}") // Шукає spring -> kafka -> consumer -> group-id
    private String groupId;

    @Bean
    public ConsumerFactory<String, OrderPlacedEventDTO> consumerFactory() {
        Map<String, Object> props = new HashMap<>();

        // 1. Явно вказуємо адресу (вирішує проблему UnknownHostException)
        props.put(ConsumerConfig.BOOTSTRAP_SERVERS_CONFIG, bootstrapServers);
        props.put(ConsumerConfig.GROUP_ID_CONFIG, groupId);
        props.put(ConsumerConfig.AUTO_OFFSET_RESET_CONFIG, "earliest");

        // 2. Налаштовуємо JSON десеріалізатор
        // Ми кажемо: "Очікуй саме клас OrderPlacedEventDTO"
        JsonDeserializer<OrderPlacedEventDTO> deserializer = new JsonDeserializer<>(OrderPlacedEventDTO.class);
        deserializer.setRemoveTypeHeaders(false);
        deserializer.addTrustedPackages("*");
        deserializer.setUseTypeMapperForKey(true);

        return new DefaultKafkaConsumerFactory<>(
                props,
                new StringDeserializer(),
                deserializer // ✅ Використовуємо налаштований JSON десеріалізатор
        );
    }

    @Bean
    public ConcurrentKafkaListenerContainerFactory<String, OrderPlacedEventDTO> kafkaListenerContainerFactory() {
        ConcurrentKafkaListenerContainerFactory<String, OrderPlacedEventDTO> factory =
                new ConcurrentKafkaListenerContainerFactory<>();
        factory.setConsumerFactory(consumerFactory());
        return factory;
    }
}