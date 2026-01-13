package com.crafthub.delivery_service.entity;

public enum DeliveryStatus {
    PREPARING,      // Оплачено, створена чернетка
    READY_TO_SHIP,  // Продавець упакував, але ще не відніс на пошту
    SHIPPED,        // Передано перевізнику (є ТТН)
    DELIVERED,      // Отримано клієнтом
    RETURNED,       // Відмова
    CANCELLED       // Скасовано
}