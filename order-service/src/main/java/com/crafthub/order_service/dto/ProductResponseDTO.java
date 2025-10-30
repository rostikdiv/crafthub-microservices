package com.crafthub.order_service.dto;

import java.math.BigDecimal;

// Використовуємо Java Record для незмінного DTO
public record ProductResponseDTO(
        Long id,
        String name,
        BigDecimal price,
        Integer stockQuantity
) {
}