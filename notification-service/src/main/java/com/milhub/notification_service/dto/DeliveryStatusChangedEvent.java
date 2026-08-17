package com.milhub.notification_service.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import java.util.UUID;

/**
 * Event record representing a change in delivery status for an order.
 */
@JsonIgnoreProperties(ignoreUnknown = true)
public record DeliveryStatusChangedEvent(
                UUID orderId,
                String status,
                String trackingNumber) {
}