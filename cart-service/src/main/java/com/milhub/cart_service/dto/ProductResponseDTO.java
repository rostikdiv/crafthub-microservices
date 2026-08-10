package com.milhub.cart_service.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import java.math.BigDecimal;
import java.util.UUID;

/**
 * External DTO representing a product from the Product Service.
 */
@JsonIgnoreProperties(ignoreUnknown = true)
public record ProductResponseDTO(
                UUID id,
                String name,
                BigDecimal price,
                Integer quantity,
                String previewImageUrl,

                UUID sellerId,
                String sellerName,
                String sellerLogoUrl) {
}