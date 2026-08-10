package com.milhub.order_service.dto.delivery;

import java.math.BigDecimal;
import java.util.UUID;

public record ReturnShipmentResponseDTO(
        UUID shipmentId,
        String trackingNumber,
        BigDecimal shippingCost) {
}
