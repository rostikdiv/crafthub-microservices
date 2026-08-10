package com.milhub.product_service.dto;

import java.util.UUID;

/**
 * Data Transfer Object containing basic seller information.
 */
public record SellerInfoDTO(
                UUID userId,
                String companyName,
                String logoUrl,
                Boolean isVerified) {
}