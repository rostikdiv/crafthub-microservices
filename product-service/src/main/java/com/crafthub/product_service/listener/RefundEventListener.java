package com.crafthub.product_service.listener;

import com.crafthub.product_service.dto.event.RefundApprovedEventDTO;
import com.crafthub.product_service.service.ProductService;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
@Slf4j
public class RefundEventListener {

    private final ProductService productService;
    private final ObjectMapper objectMapper;

    @KafkaListener(topics = "return-events", groupId = "product-service-group")
    public void handleRefundApprovedEvent(String message) {
        log.info("📥 Received refund event: {}", message);

        try {
            RefundApprovedEventDTO event = objectMapper.readValue(message, RefundApprovedEventDTO.class);

            log.info("Processing stock restoration for Product: {}, Quantity: {}", event.productId(), event.quantity());

            productService.restoreStock(event.productId(), event.quantity());

            log.info("✅ Stock restored successfully");

        } catch (Exception e) {
            log.error("❌ Failed to process refund event: {}", message, e);
        }
    }
}
