package com.crafthub.payment_service.dto;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.UUID;

public record TransactionDTO(
        UUID id,
        UUID orderId,
        UUID userId,
        BigDecimal amount,
        String status,
        LocalDateTime createdAt
) {}