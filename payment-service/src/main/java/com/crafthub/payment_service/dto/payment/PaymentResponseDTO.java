package com.crafthub.payment_service.dto.payment;

import java.util.UUID;

public record PaymentResponseDTO(
        UUID transactionId,
        String status,
        String paymentUrl
) {}