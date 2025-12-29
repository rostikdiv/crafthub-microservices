package com.crafthub.order_service.service;

import com.crafthub.order_service.entity.Order;

// Наш абстрактний "відправник"
public interface EventPublisherService {
    void publishOrderCreatedEvent(Order order);
}