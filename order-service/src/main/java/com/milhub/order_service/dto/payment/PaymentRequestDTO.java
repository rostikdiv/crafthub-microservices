package com.milhub.order_service.dto.payment;

import java.math.BigDecimal;
import java.util.UUID;

/**
 * DTO for payment request.
 */
public record PaymentRequestDTO(
                UUID orderId,
                UUID userId,
                BigDecimal amount) {
}