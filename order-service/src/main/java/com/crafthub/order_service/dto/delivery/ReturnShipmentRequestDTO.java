package com.crafthub.order_service.dto.delivery;

import com.crafthub.order_service.dto.delivery.DeliveryDetailsDTO;
import java.util.UUID;

public record ReturnShipmentRequestDTO(
                UUID orderId,
                DeliveryDetailsDTO returnAddress,
                Double weight) {
}
