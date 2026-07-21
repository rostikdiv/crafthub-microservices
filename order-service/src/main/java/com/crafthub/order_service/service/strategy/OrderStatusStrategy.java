package com.crafthub.order_service.service.strategy;

import com.crafthub.order_service.entity.Order;
import com.crafthub.order_service.entity.OrderStatus;

public interface OrderStatusStrategy {
    boolean supports(OrderStatus newStatus);
    void applyStatusChange(Order order, OrderStatus newStatus);
}
