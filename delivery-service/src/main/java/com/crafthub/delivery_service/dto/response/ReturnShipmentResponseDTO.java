package com.crafthub.delivery_service.dto.response;

import java.math.BigDecimal;
import java.util.UUID;

public record ReturnShipmentResponseDTO(
        UUID shipmentId,
        String trackingNumber,
        BigDecimal shippingCost) {
}
