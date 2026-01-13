package com.crafthub.order_service.dto.order;

import com.crafthub.order_service.dto.delivery.DeliveryDetailsDTO;
import com.crafthub.order_service.dto.order.OrderItemRequestDTO;

import java.util.List;

public record OrderRequestDTO(
        List<OrderItemRequestDTO> items,
        DeliveryDetailsDTO deliveryDetails
) {}