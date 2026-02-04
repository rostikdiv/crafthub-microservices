package com.crafthub.user_service.dto.review;

import java.time.LocalDateTime;
import java.util.UUID;

public record SellerReviewResponseDTO(
        UUID id,           // ID самого відгуку
        UUID userId,       // Хто написав
        String userName,   // Ім'я автора (щоб не робити зайві запити)
        Integer rating,    // Оцінка (1-5)
        String comment,    // Текст
        LocalDateTime createdAt // Дата створення
) {}