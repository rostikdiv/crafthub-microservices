package com.crafthub.product_service.dto;

import com.crafthub.product_service.entity.enums.AccessLevel;
import jakarta.validation.constraints.*;
import java.math.BigDecimal;
import java.util.UUID;

public record ProductRequestDTO(
        @NotBlank String name,
        String description,
        @NotNull @Positive BigDecimal price,
        @NotNull @Min(0) Integer quantity,

        @NotNull(message = "Category ID is required")
        Long categoryId, // ✅ Long

        String imageUrl,
        AccessLevel accessLevel,
        @NotNull UUID sellerId
) {}