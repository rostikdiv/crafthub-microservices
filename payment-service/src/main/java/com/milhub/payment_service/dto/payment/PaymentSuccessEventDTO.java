package com.milhub.payment_service.dto.payment;

import java.math.BigDecimal;
import java.util.UUID;

public record PaymentSuccessEventDTO(
        UUID orderId,
        String userEmail,
        BigDecimal amount
) {}