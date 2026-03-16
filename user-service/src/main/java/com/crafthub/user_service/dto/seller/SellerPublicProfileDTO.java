package com.crafthub.user_service.dto.seller;

import java.time.LocalDateTime;
import java.util.UUID;
import java.util.List;
import com.crafthub.user_service.dto.address.SellerPointDTO;

/**
 * Data Transfer Object for a seller's public profile, accessible by all users.
 */
public record SellerPublicProfileDTO(
        UUID userId,
        String companyName,
        String description,
        String logoUrl,
        Float rating,
        Integer reviewCount,
        Boolean isVerified,
        LocalDateTime registeredAt,
        List<SellerPointDTO> pickupPoints) {
}