package com.crafthub.order_service.dto;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import java.util.UUID;

public record OrderRequestDTO(
        @NotNull(message = "Product ID is required")
        UUID productId,

        @Positive(message = "Quantity must be greater than 0")
        Integer quantity
) {}