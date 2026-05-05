package com.crafthub.order_service.service;

import com.crafthub.order_service.dto.event.OrderPlacedEventDTO;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Profile;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Service;

/**
 * Service responsible for publishing events to Kafka topics.
 * This service is active only in the "local" profile.
 */
@Service
@Profile("local")
@RequiredArgsConstructor
@Slf4j
public class KafkaPublisherService {

    private final KafkaTemplate<String, Object> kafkaTemplate;

    // Topic name must match the one the Notification Service listens to
    private static final String TOPIC_NAME = "order-placed-topic";
    private static final String REFUND_TOPIC = "return-events";

    /**
     * Sends an OrderPlacedEventDTO to the Kafka topic.
     * The orderId is used as the message key to ensure ordering for a specific
     * order.
     *
     * @param event The OrderPlacedEventDTO to be sent.
     */
    public void sendOrderPlacedEvent(OrderPlacedEventDTO event) {
        log.info("Sending order event to Kafka topic '{}': {}", TOPIC_NAME, event.orderId());

        try {
            // Send message with orderId as key (to guarantee ordering)
            kafkaTemplate.send(TOPIC_NAME, event.orderId().toString(), event);
            log.info("Event sent successfully");
        } catch (Exception e) {
            log.error("Failed to send Kafka event: {}", e.getMessage());
            // TODO: Add outbox table logic if Kafka is down
        }
    }

    /**
     * Sends a RefundApprovedEventDTO to the Kafka topic.
     * The orderId is used as the message key.
     *
     * @param event The RefundApprovedEventDTO to be sent.
     */
    public void sendRefundApprovedEvent(com.crafthub.order_service.dto.event.RefundApprovedEventDTO event) {
        log.info("Sending refund event to Kafka topic '{}': {}", REFUND_TOPIC, event.orderId());
        try {
            kafkaTemplate.send(REFUND_TOPIC, event.orderId().toString(), event);
            log.info("Refund Event sent successfully");
        } catch (Exception e) {
            log.error("Failed to send Refund Kafka event", e);
        }
    }
}