package com.crafthub.order_service.dto.event;

import java.math.BigDecimal;
import java.util.UUID;

/**
 * DTO for the event published when a payment is successful.
 */
public record PaymentSuccessEventDTO(
                UUID orderId,
                String userEmail,
                BigDecimal amount) {
}