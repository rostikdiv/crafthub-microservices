package com.milhub.notification_service.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import java.math.BigDecimal;
import java.util.UUID;

/**
 * Data Transfer Object representing a successful payment event.
 */
@JsonIgnoreProperties(ignoreUnknown = true)
public record PaymentSuccessEventDTO(
                UUID orderId,
                String userEmail,
                BigDecimal amount) {
}