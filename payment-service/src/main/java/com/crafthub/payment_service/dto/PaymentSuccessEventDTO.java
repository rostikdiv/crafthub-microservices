package com.crafthub.payment_service.dto;

import java.math.BigDecimal;
import java.util.UUID;

public record PaymentSuccessEventDTO(
        UUID orderId,
        String userEmail,
        BigDecimal amount
) {}