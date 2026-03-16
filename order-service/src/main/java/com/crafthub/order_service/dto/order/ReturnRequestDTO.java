package com.crafthub.order_service.dto.order;

import com.crafthub.order_service.dto.delivery.DeliveryDetailsDTO;
import jakarta.validation.constraints.NotNull;

/**
 * DTO for product return request.
 */
public record ReturnRequestDTO(
                @NotNull String orderItemId,
                @NotNull Integer quantity,
                @NotNull String reason,
                @NotNull DeliveryDetailsDTO returnAddress) {
}
