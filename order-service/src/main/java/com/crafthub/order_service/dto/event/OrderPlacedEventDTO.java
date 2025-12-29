package com.crafthub.order_service.dto.event;

import java.math.BigDecimal;
import java.util.UUID;

public record OrderPlacedEventDTO(
        UUID orderId,
        UUID userId,
        String userEmail, // Email користувача (потрібно передати з контролера)
        String productName,
        BigDecimal totalPrice
) {}