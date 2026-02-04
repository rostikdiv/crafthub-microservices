package com.crafthub.product_service.dto;

import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;

public record ProductResponseDTO(
        UUID id,
        String name,
        String description,
        BigDecimal price,
        Integer quantity,
        String categoryName,
        String accessLevel,

        UUID sellerId,
        String sellerName,
        String sellerLogoUrl,

        Double averageRating,
        Integer reviewCount,

        Double weight,
        Double length,
        Double width,
        Double height,
        String previewImageUrl,
        List<String> imageUrls
) {}