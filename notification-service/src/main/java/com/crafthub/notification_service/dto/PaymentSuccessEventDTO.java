package com.crafthub.notification_service.dto;

import java.math.BigDecimal;
import java.util.UUID;

/**
 * Data Transfer Object representing a successful payment event.
 */
public record PaymentSuccessEventDTO(
                UUID orderId,
                String userEmail,
                BigDecimal amount) {
}