package com.crafthub.order_service.listener;

import com.crafthub.order_service.dto.event.DeliveryStatusChangedEvent;
import com.crafthub.order_service.service.OrderService;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
@Slf4j
public class DeliveryStatusListener {

    private final OrderService orderService;
    private final ObjectMapper objectMapper;

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