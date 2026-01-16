package com.crafthub.notification_service.dto;

import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;

// ❗️ Порядок полів має бути ідентичним OrderService
public record OrderPlacedEventDTO(
        UUID orderId,
        UUID userId,
        String userEmail,
        BigDecimal totalPrice,
        String productName,
        List<UUID> productIds // ✅ Додайте це поле, навіть якщо Notification його не використовує
) {}