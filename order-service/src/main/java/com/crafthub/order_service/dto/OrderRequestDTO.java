package com.crafthub.order_service.dto;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotEmpty;
import java.util.List;

public record OrderRequestDTO(
        @NotEmpty(message = "Order must contain at least one item")
        @Valid // ❗️ Вказує Spring валідувати об'єкти всередині списку
        List<OrderItemRequestDTO> items
) {
}