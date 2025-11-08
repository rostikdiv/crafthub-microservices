package com.crafthub.order_service.service;

import com.crafthub.order_service.event.OrderCreatedEvent;
import com.crafthub.order_service.model.Order;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Profile;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
@Slf4j
@Profile("local") // ❗️ Завантажувати, т-ільки якщо активний профіль "kafka"
public class KafkaPublisherService implements EventPublisherService {

    private final KafkaTemplate<String, String> kafkaTemplate;
    private final ObjectMapper objectMapper;
    private static final String ORDERS_TOPIC = "orders_topic";

    @Override
    public void publishOrderCreatedEvent(Order order) {
        try {
            OrderCreatedEvent event = new OrderCreatedEvent(
                    order.getOrderNumber(),
                    order.getUserId(),
                    order.getTotalPrice()
            );
            String message = objectMapper.writeValueAsString(event);
            kafkaTemplate.send(ORDERS_TOPIC, message);
            log.info("Order Created event published to KAFKA: {}", message);
        } catch (Exception e) {
            log.error("Failed to publish OrderCreatedEvent to KAFKA: {}", e.getMessage());
        }
    }
}