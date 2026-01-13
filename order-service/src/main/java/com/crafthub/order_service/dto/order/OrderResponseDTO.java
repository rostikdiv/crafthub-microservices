package com.crafthub.order_service.dto.order;

import com.crafthub.order_service.dto.delivery.DeliveryDetailsDTO; // ✅ Не забудьте імпорт
import com.crafthub.order_service.entity.OrderStatus;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

public record OrderResponseDTO(
        UUID id,
        UUID userId,
        BigDecimal totalPrice,
        OrderStatus status,
        LocalDateTime createdAt,
        List<OrderItemResponseDTO> items,
        DeliveryDetailsDTO deliveryInfo // ✅ Додаємо це поле!
) {}