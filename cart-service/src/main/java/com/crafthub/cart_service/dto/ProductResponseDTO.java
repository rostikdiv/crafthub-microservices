package com.crafthub.cart_service.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties; // ✅ Імпорт
import java.math.BigDecimal;
import java.util.UUID;

// Анотація каже: "Якщо Product Service надіслав поля, яких тут немає (наприклад weight, description) - просто проігноруй їх і не ламайся".
@JsonIgnoreProperties(ignoreUnknown = true)
public record ProductResponseDTO(
        UUID id,
        String name,
        BigDecimal price,
        Integer quantity,
        String previewImageUrl
) {}