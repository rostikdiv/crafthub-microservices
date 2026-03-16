package com.crafthub.order_service.dto.event;

import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;

/**
 * DTO for the event published when an order is officially placed.
 */
public record OrderPlacedEventDTO(
                UUID orderId,
                UUID userId,
                String userEmail,
                BigDecimal totalPrice,
                String productName, // Summary of product names
                List<UUID> productIds // List of IDs to clear from cart
) {
}