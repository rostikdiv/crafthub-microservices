package com.crafthub.order_service.dto.event;

import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;

public record OrderPlacedEventDTO(
        UUID orderId,
        UUID userId,
        String userEmail,       // 3. Email
        BigDecimal totalPrice,  // 4. Ціна (в OrderService вона йде перед назвою)
        String productName,     // 5. Назва товарів (summary)
        List<UUID> productIds   // 6. Список ID для очищення кошика
) {}