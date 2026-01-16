package com.crafthub.cart_service.dto;

import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;

// Порядок полів має бути таким же, як в Order Service!
public record OrderPlacedEventDTO(
        UUID orderId,
        UUID userId,
        String userEmail,
        BigDecimal totalPrice,
        String productName,
        List<UUID> productIds // ✅ Список ID куплених товарів
) {}