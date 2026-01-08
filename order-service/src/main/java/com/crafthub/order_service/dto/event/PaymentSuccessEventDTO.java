package com.crafthub.order_service.dto.event;

import java.math.BigDecimal;
import java.util.UUID;

public record PaymentSuccessEventDTO(
        UUID orderId,
        String userEmail,
        BigDecimal amount
) {}