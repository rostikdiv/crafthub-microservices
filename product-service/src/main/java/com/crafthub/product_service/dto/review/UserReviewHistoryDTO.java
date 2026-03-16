package com.crafthub.product_service.dto.review;

import java.time.LocalDateTime;
import java.util.UUID;

/**
 * Data Transfer Object for user review history.
 */
public record UserReviewHistoryDTO(
                UUID id,
                String comment, // User's comment text
                Integer rating, // User's rating (may be null for replies)
                LocalDateTime createdAt,

                // Product information
                UUID productId,
                String productName,
                String productImageUrl,

                // Context for replies
                boolean isReply, // true if this is a reply to another review
                String replyToUserName, // Name of the user being replied to
                String replyToText // Truncated preview of the parent comment
) {
}