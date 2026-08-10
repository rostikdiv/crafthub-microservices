package com.milhub.order_service.dto.order;

import com.milhub.order_service.dto.delivery.DeliveryDetailsDTO;
import com.milhub.order_service.entity.OrderStatus;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

/**
 * DTO for order response.
 */
public record OrderResponseDTO(
                UUID id,
                UUID userId,
                BigDecimal totalPrice,
                OrderStatus status,
                LocalDateTime createdAt,
                List<OrderItemResponseDTO> items,
                DeliveryDetailsDTO deliveryInfo) {
}