package com.crafthub.payment_service.dto;

import java.util.UUID;

public record PaymentResponseDTO(
        UUID transactionId,
        String status,
        String paymentUrl
) {}