package com.crafthub.delivery_service.dto.response;

import java.math.BigDecimal;
import java.util.UUID;

/**
 * Data Transfer Object for carrying return shipment response details.
 */
public record ReturnShipmentResponseDTO(
                UUID shipmentId,
                String trackingNumber,
                BigDecimal shippingCost) {
}
