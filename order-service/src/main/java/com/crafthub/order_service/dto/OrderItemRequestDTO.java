package com.crafthub.order_service.dto;

import java.util.UUID;

public record OrderItemRequestDTO(
        UUID productId,
        Integer quantity
) {}