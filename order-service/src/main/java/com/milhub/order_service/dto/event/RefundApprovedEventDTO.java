package com.milhub.order_service.dto.event;

import java.util.UUID;

/**
 * DTO for the event published when a refund is approved.
 */
public record RefundApprovedEventDTO(
                UUID orderId,
                UUID productId,
                Integer quantity,
                String reason) {
}
