package com.milhub.order_service.dto.order;

import com.milhub.order_service.dto.delivery.DeliveryDetailsDTO;
import com.milhub.order_service.entity.enums.PaymentMethod;
import java.util.List;

/**
 * DTO for creating a new order.
 */
public record OrderRequestDTO(
        List<OrderItemRequestDTO> items,
        DeliveryDetailsDTO deliveryDetails,
        PaymentMethod paymentMethod) {
}