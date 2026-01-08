package com.crafthub.order_service.dto;

import java.math.BigDecimal;
import java.util.UUID;

public record OrderItemResponseDTO(
        UUID productId,
        Integer quantity,
        BigDecimal pricePerUnit
) {}