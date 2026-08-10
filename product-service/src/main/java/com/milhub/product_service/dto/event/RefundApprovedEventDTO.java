package com.milhub.product_service.dto.event;

import java.util.UUID;

/**
 * Event DTO representing a refund approval.
 */
public record RefundApprovedEventDTO(
                UUID orderId,
                UUID productId,
                Integer quantity,
                String reason) {
}
