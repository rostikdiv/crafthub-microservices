package com.milhub.notification_service.dto;

import java.util.UUID;

/**
 * Event record representing a change in delivery status for an order.
 */
public record DeliveryStatusChangedEvent(
                UUID orderId,
                String status,
                String trackingNumber) {
}