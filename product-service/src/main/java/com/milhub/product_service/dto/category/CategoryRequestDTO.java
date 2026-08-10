package com.milhub.product_service.dto.category;

import jakarta.validation.constraints.NotBlank;

/**
 * Request DTO for creating or updating a category.
 */
public record CategoryRequestDTO(
                @NotBlank(message = "Name is required") String name,
                String description,
                Long parentId) {
}