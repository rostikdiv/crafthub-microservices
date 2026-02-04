package com.crafthub.user_service.dto;

import java.time.LocalDateTime;
import java.util.UUID;

public record SellerPublicProfileDTO(
        UUID userId,
        String companyName,
        String description,
        String logoUrl,
        Float rating,
        Integer reviewCount, // 👈 Додаємо сюди
        Boolean isVerified,
        LocalDateTime registeredAt
) {}