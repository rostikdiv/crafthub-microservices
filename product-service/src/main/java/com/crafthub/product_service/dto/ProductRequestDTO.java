package com.crafthub.product_service.dto;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import java.math.BigDecimal;

// Використовуємо record для DTO запиту
public record ProductRequestDTO(
        @NotEmpty(message = "Product name cannot be empty")
        String name,

        String description,

        @NotNull(message = "Price cannot be null")
        @DecimalMin(value = "0.01", message = "Price must be greater than 0")
        BigDecimal price,

        @NotNull(message = "Stock quantity cannot be null")
        @Min(value = 0, message = "Stock quantity cannot be negative")
        Integer stockQuantity
) {
}