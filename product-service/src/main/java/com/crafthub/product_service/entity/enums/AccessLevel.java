package com.crafthub.product_service.entity.enums;

public enum AccessLevel {
    PUBLIC,     // Доступно всім
    RESTRICTED  // Тільки для військових (Фронтенд блокує кнопку, Order Service блокує замовлення)
}