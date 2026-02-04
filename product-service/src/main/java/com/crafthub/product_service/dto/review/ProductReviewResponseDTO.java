package com.crafthub.product_service.dto.review;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

public record ProductReviewResponseDTO(
        UUID id,
        UUID userId,
        String userName,
        Integer rating,
        String comment,
        boolean isVerifiedPurchase,
        LocalDateTime createdAt,
        UUID parentId, // ID батька (якщо є)
        List<ProductReviewResponseDTO> replies // Вкладені відповіді
) {}