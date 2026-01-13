package com.crafthub.delivery_service.dto.response;

import com.crafthub.delivery_service.dto.external.DeliveryDetailsDTO;
import com.crafthub.delivery_service.entity.DeliveryStatus;
import lombok.Builder;
import lombok.Data;

import java.time.LocalDateTime;
import java.util.UUID;

@Data
@Builder
public class ShipmentResponseDTO {
    private UUID id;
    private UUID orderId;
    private DeliveryStatus status;
    private String trackingNumber;
    private DeliveryDetailsDTO deliveryDetails; // Покажемо повну адресу
    private LocalDateTime createdAt;
    private LocalDateTime shippedAt;
}