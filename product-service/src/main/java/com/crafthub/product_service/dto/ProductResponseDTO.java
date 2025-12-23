package com.crafthub.product_service.dto;

import com.crafthub.product_service.entity.enums.AccessLevel;
import lombok.Builder;
import java.math.BigDecimal;
import java.util.UUID;

@Builder
public record ProductResponseDTO(
        UUID id,
        String name,
        String description,
        BigDecimal price,
        Integer quantity,
        String categoryName,
        Long categoryId,     // ✅ Long
        String imageUrl,
        AccessLevel accessLevel,
        UUID sellerId
) {}