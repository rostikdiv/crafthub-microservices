package com.milhub.order_service.dto.order;

import java.util.UUID;

/**
 * DTO for order item request.
 */
public record OrderItemRequestDTO(
                UUID productId,
                Integer quantity) {
}