package com.crafthub.order_service.exception;

// Кастомна помилка для заборони доступу
public class AccessDeniedException extends RuntimeException {
    public AccessDeniedException(String message) {
        super(message);
    }
}