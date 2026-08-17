package com.milhub.product_service.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import java.util.UUID;

/**
 * Data Transfer Object containing basic seller information.
 */
@JsonIgnoreProperties(ignoreUnknown = true)
public record SellerInfoDTO(
                UUID userId,
                String companyName,
                String logoUrl,
                Boolean isVerified) {
}