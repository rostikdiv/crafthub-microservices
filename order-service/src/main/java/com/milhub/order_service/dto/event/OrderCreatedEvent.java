package com.milhub.order_service.dto.event;

import java.math.BigDecimal;

/**
 * Kafka event published when a new order is created.
 */
public record OrderCreatedEvent(
        String orderNumber,
        String userEmail,
        BigDecimal totalPrice) {
}
