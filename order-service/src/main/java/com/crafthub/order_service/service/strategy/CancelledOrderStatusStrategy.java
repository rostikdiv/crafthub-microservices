package com.crafthub.order_service.service.strategy;

import com.crafthub.order_service.entity.Order;
import com.crafthub.order_service.entity.OrderStatus;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

@Component
@Slf4j
public class CancelledOrderStatusStrategy implements OrderStatusStrategy {

    @Override
    public boolean supports(OrderStatus newStatus) {
        return newStatus == OrderStatus.CANCELLED;
    }

    @Override
    public void applyStatusChange(Order order, OrderStatus newStatus) {
        log.info("Applying CANCELLED status to Order {}", order.getId());
        order.setStatus(newStatus);
    }
}
