package com.crafthub.order_service.service;

import com.crafthub.order_service.dto.event.OrderPlacedEventDTO; // ✅
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Profile;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Service;

@Service
@Profile("local")
@RequiredArgsConstructor
@Slf4j
public class KafkaPublisherService {

    private final KafkaTemplate<String, OrderPlacedEventDTO> kafkaTemplate;

    // Назва топіку має співпадати з тією, яку слухає Notification Service
    private static final String TOPIC_NAME = "order-placed-topic";

    public void sendOrderPlacedEvent(OrderPlacedEventDTO event) {
        log.info("📤 Sending order event to Kafka topic '{}': {}", TOPIC_NAME, event.orderId());

        try {
            // Відправляємо повідомлення, де ключ - це orderId (для гарантії порядку)
            kafkaTemplate.send(TOPIC_NAME, event.orderId().toString(), event);
            log.info("✅ Event sent successfully");
        } catch (Exception e) {
            log.error("❌ Failed to send Kafka event: {}", e.getMessage());
            // Тут можна додати логіку збереження в Outbox таблицю, якщо Kafka лежить
        }
    }
}