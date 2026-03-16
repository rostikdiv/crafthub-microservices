package com.crafthub.product_service.dto.category;

import lombok.Builder;

/**
 * Response DTO containing category details.
 */
@Builder
public record CategoryResponseDTO(
        Long id,
        String name,
        String description) {
}