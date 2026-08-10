package com.milhub.order_service.listener;

import com.milhub.order_service.dto.event.DeliveryStatusChangedEvent;
import com.milhub.order_service.service.OrderService;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;

/**
 * Listener for delivery status update events from Kafka.
 */
@Component
@RequiredArgsConstructor
@Slf4j
public class DeliveryStatusListener {

    private final OrderService orderService;
    private final ObjectMapper objectMapper;

    /**
     * Handles delivery status change events.
     * Parses the message and updates the corresponding order status.
     *
     * @param message The JSON message from Kafka.
     */
    @KafkaListener(topics = "delivery-status-topic", groupId = "order-service-group")
    public void handleDeliveryStatusChange(String message) {
        try {
            log.info("📨 Received delivery status update: {}", message);
            DeliveryStatusChangedEvent event = objectMapper.readValue(message, DeliveryStatusChangedEvent.class);

            orderService.updateOrderStatusFromDelivery(event.orderId(), event.status());

        } catch (Exception e) {
            log.error("❌ Error processing delivery status event", e);
        }
    }
}