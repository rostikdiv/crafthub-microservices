package com.milhub.delivery_service.dto.event;

import com.milhub.delivery_service.entity.DeliveryStatus;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.UUID;

/**
 * Event published when a shipment's delivery status changes.
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class DeliveryStatusChangedEvent {
    private UUID orderId; // Link to Order Service
    private DeliveryStatus status; // New status (e.g., PREPARING, SHIPPED)
    private LocalDateTime timestamp;
}