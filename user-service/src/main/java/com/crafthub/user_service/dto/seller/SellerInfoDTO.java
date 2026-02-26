package com.crafthub.user_service.dto;

import java.util.UUID;

public record SellerInfoDTO(
                UUID userId,
                String companyName,
                String logoUrl,
                Boolean isVerified,
                Float rating,
                Integer reviewCount,
                Integer totalSales) {
}