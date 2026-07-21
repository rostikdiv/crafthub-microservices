package com.crafthub.order_service.service.strategy;

import com.crafthub.order_service.entity.Order;
import com.crafthub.order_service.entity.OrderStatus;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

@Component
@Slf4j
public class DefaultOrderStatusStrategy implements OrderStatusStrategy {

    @Override
    public boolean supports(OrderStatus newStatus) {
        // Will be used as a fallback if no specific strategy exists
        return false; 
    }

    @Override
    public void applyStatusChange(Order order, OrderStatus newStatus) {
        log.info("Applying {} status to Order {}", newStatus, order.getId());
        order.setStatus(newStatus);
    }
}
