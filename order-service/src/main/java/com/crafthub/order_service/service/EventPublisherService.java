package com.crafthub.order_service.service;

import com.crafthub.order_service.event.OrderCreatedEvent;
import com.crafthub.order_service.model.Order;

// Наш абстрактний "відправник"
public interface EventPublisherService {
    void publishOrderCreatedEvent(Order order);
}