package com.crafthub.order_service.dto.order;

import java.util.UUID;

public record OrderItemRequestDTO(
        UUID productId,
        Integer quantity
) {}