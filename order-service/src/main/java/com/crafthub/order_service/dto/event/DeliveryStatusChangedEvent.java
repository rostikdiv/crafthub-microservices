package com.crafthub.order_service.dto.event;

import java.time.LocalDateTime;
import java.util.UUID;

/**
 * Event published when the delivery status of an order changes.
 */
public record DeliveryStatusChangedEvent(
        UUID orderId,
        String status, // Received as a string (e.g., "SHIPPED", "READY_TO_SHIP")
        LocalDateTime timestamp) {
}