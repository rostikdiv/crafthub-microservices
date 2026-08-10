package com.milhub.product_service.dto.product;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.PositiveOrZero;

import java.math.BigDecimal;
import java.util.List;

/**
 * Request DTO for creating or updating a product.
 */
public record ProductRequestDTO(
        @NotBlank(message = "Name is required") String name,

        String description,

        @NotNull(message = "Price is required") @Positive(message = "Price must be greater than 0") BigDecimal price,

        @NotNull(message = "Quantity is required") @PositiveOrZero(message = "Quantity cannot be negative") Integer quantity,

        @NotNull(message = "Category ID is required") Long categoryId,

        @NotBlank(message = "Access level is required") String accessLevel,

        @NotNull(message = "Weight is required") @Positive Double weight,

        @NotNull(message = "Length is required") @Positive Double length,

        @NotNull(message = "Width is required") @Positive Double width,

        @NotNull(message = "Height is required") @Positive Double height,

        @NotBlank(message = "Preview image is required") String previewImageUrl,

        List<String> imageUrls) {
}