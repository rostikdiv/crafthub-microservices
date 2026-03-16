package com.crafthub.order_service.dto.order;

import java.math.BigDecimal;
import java.util.UUID;

/**
 * DTO for product return response.
 */
public record ReturnResponseDTO(
                UUID returnId,
                BigDecimal finalRefundAmount,
                String trackingNumber,
                BigDecimal returnShippingCost,
                String status) {
}
