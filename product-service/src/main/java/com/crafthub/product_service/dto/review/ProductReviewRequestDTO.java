package com.crafthub.product_service.dto.review;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import java.util.UUID;

public record ProductReviewRequestDTO(
        @NotNull(message = "Product ID is required")
        UUID productId,

        // Рейтинг 1-5. Може бути null, якщо це відповідь на коментар.
        @Min(value = 1, message = "Rating must be at least 1")
        @Max(value = 5, message = "Rating must be at most 5")
        Integer rating,

        @NotBlank(message = "Comment cannot be empty")
        String comment,

        // ID батьківського коментаря (якщо це відповідь)
        UUID parentId
) {}