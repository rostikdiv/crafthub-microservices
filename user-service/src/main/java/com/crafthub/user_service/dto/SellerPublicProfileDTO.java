package com.crafthub.user_service.dto;

import java.time.LocalDateTime;
import java.util.UUID;

public record SellerPublicProfileDTO(
                UUID userId,
                String companyName,
                String description,
                String logoUrl,
                Float rating,
                Integer reviewCount,
                Boolean isVerified,
                LocalDateTime registeredAt,
                java.util.List<com.crafthub.user_service.dto.address.SellerPointDTO> pickupPoints // 👈 Новое поле
) {
}