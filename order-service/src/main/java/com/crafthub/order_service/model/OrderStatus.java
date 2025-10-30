package com.crafthub.order_service.model;

public enum OrderStatus {
    PENDING,        // Очікує
    PROCESSING,     // В обробці
    SHIPPED,        // Відправлено
    DELIVERED,      // Доставлено
    CANCELLED       // Скасовано
}