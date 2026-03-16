package com.crafthub.product_service.dto.review;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

/**
 * Response DTO containing product review details and its replies.
 */
public record ProductReviewResponseDTO(
                UUID id,
                UUID userId,
                String userName,
                Integer rating,
                String comment,
                boolean isVerifiedPurchase,
                LocalDateTime createdAt,
                UUID parentId, // Parent ID (if any)
                List<ProductReviewResponseDTO> replies // Nested replies
) {
}