package com.crafthub.user_service.dto.review;

import java.time.LocalDateTime;
import java.util.UUID;

/**
 * Data Transfer Object representing a response containing review details.
 */
public record SellerReviewResponseDTO(
                UUID id,
                UUID reviewerId,
                String reviewerName,
                Integer rating,
                String comment,
                LocalDateTime createdAt) {
}