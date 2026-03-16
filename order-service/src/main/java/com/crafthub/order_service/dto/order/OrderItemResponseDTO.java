package com.crafthub.order_service.dto.order;

import java.math.BigDecimal;
import java.util.UUID;

/**
 * DTO for order item response.
 */
public record OrderItemResponseDTO(
        UUID productId,
        String name,
        Integer quantity,
        BigDecimal pricePerUnit) {
}