package com.crafthub.order_service.dto.payment;

import java.math.BigDecimal;
import java.util.UUID;

public record PaymentRequestDTO(
        UUID orderId,
        UUID userId,
        BigDecimal amount
) {}