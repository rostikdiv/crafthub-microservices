package com.crafthub.product_service.dto.event;

import java.util.UUID;

public record RefundApprovedEventDTO(
        UUID orderId,
        UUID productId,
        Integer quantity,
        String reason) {
}
