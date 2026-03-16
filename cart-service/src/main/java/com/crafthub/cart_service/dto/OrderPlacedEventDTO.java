package com.crafthub.cart_service.dto;

import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;

/**
 * DTO representing an order placement event received from Kafka.
 */
public record OrderPlacedEventDTO(
                UUID orderId,
                UUID userId,
                String userEmail,
                BigDecimal totalPrice,
                String productName,
                List<UUID> productIds) {
}