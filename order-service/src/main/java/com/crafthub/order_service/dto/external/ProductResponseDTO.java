package com.crafthub.order_service.dto.external;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import java.math.BigDecimal;
import java.util.UUID;

// ✅ Додаємо ігнорування невідомих полів, щоб не ламало серіалізацію
@JsonIgnoreProperties(ignoreUnknown = true)
public record ProductResponseDTO(
        UUID id,
        String name,
        BigDecimal price,
        String accessLevel,
        Integer quantity,
        UUID sellerId
) {}