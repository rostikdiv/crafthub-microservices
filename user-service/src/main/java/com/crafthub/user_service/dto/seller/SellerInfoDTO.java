package com.crafthub.user_service.dto.seller;

import java.util.UUID;

/**
 * Data Transfer Object containing basic seller information and statistics.
 */
public record SellerInfoDTO(
        UUID userId,
        String companyName,
        String logoUrl,
        Boolean isVerified,
        Float rating,
        Integer reviewCount,
        Integer totalSales) {
}