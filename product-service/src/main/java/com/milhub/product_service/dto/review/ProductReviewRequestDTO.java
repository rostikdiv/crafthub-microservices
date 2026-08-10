package com.milhub.product_service.dto.review;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

import java.util.UUID;

/**
 * Data Transfer Object for creating or replying to a product review.
 */
public record ProductReviewRequestDTO(
                @NotNull(message = "Product ID is required") UUID productId,

                // Rating from 1 to 5. Can be null if it's a reply to a comment.
                @Min(value = 1, message = "Rating must be at least 1") @Max(value = 5, message = "Rating must be at most 5") Integer rating,

                @NotBlank(message = "Comment cannot be empty") String comment,

                // ID of the parent comment (if this is a reply)
                UUID parentId) {
}