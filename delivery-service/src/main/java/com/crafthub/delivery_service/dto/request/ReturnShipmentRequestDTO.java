package com.crafthub.delivery_service.dto.request;

import com.crafthub.delivery_service.dto.external.DeliveryDetailsDTO;
import jakarta.validation.constraints.NotNull;
import java.util.UUID;

/**
 * Data Transfer Object for requesting a return shipment.
 */
public record ReturnShipmentRequestDTO(
                @NotNull UUID orderId,
                @NotNull DeliveryDetailsDTO returnAddress, // Pickup or delivery address for return
                Double weight) {
}
