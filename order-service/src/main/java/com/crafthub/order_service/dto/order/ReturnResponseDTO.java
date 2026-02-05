package com.crafthub.order_service.dto.order;

import java.math.BigDecimal;
import java.util.UUID;

public record ReturnResponseDTO(
        UUID returnId,
        BigDecimal finalRefundAmount,
        String trackingNumber,
        BigDecimal returnShippingCost,
        String status) {
}
