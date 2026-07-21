package com.crafthub.payment_service.dto.payment;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import java.math.BigDecimal;
import java.util.UUID;

public record PaymentRequestDTO(
        @NotNull UUID orderId,
        @NotNull UUID userId,
        @Positive BigDecimal amount,
        String idempotencyKey
) {}