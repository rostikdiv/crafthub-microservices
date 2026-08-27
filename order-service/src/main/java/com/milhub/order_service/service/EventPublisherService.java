package com.milhub.order_service.service;

import com.milhub.order_service.entity.Order;

// Abstract event publisher
public interface EventPublisherService {
    void publishOrderCreatedEvent(Order order);
}