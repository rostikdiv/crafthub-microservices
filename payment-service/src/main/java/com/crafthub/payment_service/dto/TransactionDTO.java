package com.crafthub.payment_service.dto;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.UUID;

/**
 * Data Transfer Object representing a payment transaction's details.
 */
public record TransactionDTO(
                UUID id,
                UUID orderId,
                UUID userId,
                BigDecimal amount,
                String status,
                LocalDateTime createdAt) {
}