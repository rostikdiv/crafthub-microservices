package com.milhub.order_service.service.strategy;

import com.milhub.order_service.entity.Order;
import com.milhub.order_service.entity.OrderStatus;

public interface OrderStatusStrategy {
    boolean supports(OrderStatus newStatus);
    void applyStatusChange(Order order, OrderStatus newStatus);
}
