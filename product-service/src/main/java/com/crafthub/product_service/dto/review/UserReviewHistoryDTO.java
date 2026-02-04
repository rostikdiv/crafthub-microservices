package com.crafthub.product_service.dto.review;

import java.time.LocalDateTime;
import java.util.UUID;

public record UserReviewHistoryDTO(
        UUID id,
        String comment,          // Мій текст
        Integer rating,          // Моя оцінка (може бути null, якщо це просто відповідь)
        LocalDateTime createdAt,

        // Інформація про товар (щоб знати, де я це написав)
        UUID productId,
        String productName,
        String productImageUrl,

        // Контекст (якщо це відповідь)
        boolean isReply,         // true, якщо це відповідь
        String replyToUserName,  // Кому я відповів
        String replyToText       // Текст, на який я відповів (прев'ю)
) {}