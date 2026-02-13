package com.crafthub.order_service.dto.event;

import java.time.LocalDateTime;
import java.util.UUID;

public record DeliveryStatusChangedEvent(
                UUID orderId,
                String status, // Прийде як рядок ("SHIPPED", "READY_TO_SHIP")
                LocalDateTime timestamp) {
}