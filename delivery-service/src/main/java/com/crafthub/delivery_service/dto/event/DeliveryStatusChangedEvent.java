package com.crafthub.delivery_service.dto.event;

import com.crafthub.delivery_service.entity.DeliveryStatus;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.UUID;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class DeliveryStatusChangedEvent {
    private UUID orderId;          // Ключ для Order Service
    private DeliveryStatus status; // Новий статус (PREPARING, SHIPPED...)
    private LocalDateTime timestamp;
}