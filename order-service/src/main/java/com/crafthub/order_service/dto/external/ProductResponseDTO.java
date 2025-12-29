package com.crafthub.order_service.dto.external;

import java.math.BigDecimal;
import java.util.UUID;

// Використовуємо Record (DTO)
public record ProductResponseDTO(
        UUID id,
        String name,
        BigDecimal price,
        String accessLevel, // Нам важливе це поле (прийде як String)
        Integer quantity
) {}