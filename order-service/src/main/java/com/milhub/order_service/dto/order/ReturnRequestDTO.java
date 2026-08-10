package com.milhub.order_service.dto.order;

import com.milhub.order_service.dto.delivery.DeliveryDetailsDTO;
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
