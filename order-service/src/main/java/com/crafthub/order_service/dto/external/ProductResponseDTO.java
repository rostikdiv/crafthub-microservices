package com.crafthub.order_service.dto.external;

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
                String accessLevel,
                Integer quantity,
                UUID sellerId) {
}