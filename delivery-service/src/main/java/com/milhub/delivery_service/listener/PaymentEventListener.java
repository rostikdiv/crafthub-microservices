package com.milhub.delivery_service.listener;

import com.milhub.delivery_service.service.ShipmentService;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;

/**
 * Listener for payment-related events from Kafka.
 * Triggers shipment creation upon successful payment.
 */
@Component
@RequiredArgsConstructor
@Slf4j
public class PaymentEventListener {

    private final ShipmentService shipmentService;
    private final ObjectMapper objectMapper;

    @KafkaListener(topics = "payment-success-topic", groupId = "delivery-service-group")
    public void handlePaymentSuccess(String message) {
        try {
            log.info("📨 Received payment success event: {}", message);

            // Extract orderId from the JSON message
            JsonNode jsonNode = objectMapper.readTree(message);
            String orderIdStr = jsonNode.get("orderId").asText();

            shipmentService.createShipment(java.util.UUID.fromString(orderIdStr));

        } catch (Exception e) {
            log.error("Error processing payment event in Delivery Service", e);
        }
    }
}