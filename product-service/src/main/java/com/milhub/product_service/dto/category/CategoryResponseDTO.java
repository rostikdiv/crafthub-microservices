package com.milhub.product_service.dto.category;

import lombok.Builder;

import java.util.List;

/**
 * Response DTO containing category details.
 */
@Builder
public record CategoryResponseDTO(
        Long id,
        String name,
        String description,
        Long parentId,
        List<CategoryResponseDTO> subCategories) {
}