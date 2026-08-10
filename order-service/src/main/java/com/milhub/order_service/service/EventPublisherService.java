package com.milhub.order_service.service;

import com.milhub.order_service.entity.Order;

// Наш абстрактний "відправник"
public interface EventPublisherService {
    void publishOrderCreatedEvent(Order order);
}