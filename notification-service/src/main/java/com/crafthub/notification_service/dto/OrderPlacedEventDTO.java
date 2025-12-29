package com.crafthub.notification_service.dto;

import java.math.BigDecimal;
import java.util.UUID;

public record OrderPlacedEventDTO(
        UUID orderId,
        UUID userId,
        String userEmail,
        String productName,
        BigDecimal totalPrice
) {}