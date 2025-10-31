package com.crafthub.order_service.event;

import java.math.BigDecimal;

// Використовуємо Record для простого та незмінного DTO,
// що ідеально підходить для подій Kafka.
public record OrderCreatedEvent(
        String orderNumber,
        String userEmail,
        BigDecimal totalPrice
) {
    // В Spring Boot 3+ ObjectMapper автоматично знайде цей конструктор
}