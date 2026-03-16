package com.crafthub.payment_service.exception;

/**
 * Exception thrown for business logic violations in the Payment Service.
 */
public class BusinessException extends RuntimeException {
    public BusinessException(String message) {
        super(message);
    }
}