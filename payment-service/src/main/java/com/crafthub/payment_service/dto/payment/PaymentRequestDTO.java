package com.crafthub.payment_service.dto;

import java.math.BigDecimal;
import java.util.UUID;

public record PaymentRequestDTO(
        UUID orderId,
        UUID userId,
        BigDecimal amount
) {}