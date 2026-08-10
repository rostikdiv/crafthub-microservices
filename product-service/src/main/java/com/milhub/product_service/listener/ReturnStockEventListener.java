package com.milhub.product_service.listener;

import com.milhub.product_service.service.ProductService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;

import java.util.UUID;

/**
 * Listener for Kafka events that require compensating transactions (Saga Pattern),
 * such as restoring inventory stock when an order is cancelled or a return is approved.
 */
@Component
@RequiredArgsConstructor
@Slf4j
public class ReturnStockEventListener {

    private final ProductService productService;

    // We assume the event contains productId and quantity to restore.
    // In a real scenario, this would be a specific DTO object mapped from JSON.
    public record StockReturnEventDTO(UUID productId, Integer quantity, String reason) {}

    @KafkaListener(topics = {"order-failed-events", "return-events"}, groupId = "product-service-group")
    public void handleStockReturn(StockReturnEventDTO event) {
        log.info("Received stock return event for product {}: +{} (Reason: {})", 
                 event.productId(), event.quantity(), event.reason());
                 
        try {
            productService.restoreStock(event.productId(), event.quantity());
            log.info("Stock restored successfully for product {}", event.productId());
        } catch (Exception e) {
            log.error("Failed to restore stock for product {}. Manual intervention may be required. Error: {}", 
                      event.productId(), e.getMessage());
            // In a fully robust Saga, we might push this to a Dead Letter Queue (DLQ)
        }
    }
}
