package com.crafthub.cart_service.dto;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotEmpty;

// Використовуємо record для простоти
public record CartItemRequestDTO(
        @NotEmpty(message = "Product ID is required")
        String productId,

        @Min(value = 1, message = "Quantity must be at least 1")
        Integer quantity
) {
}