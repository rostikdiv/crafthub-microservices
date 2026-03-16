package com.crafthub.cart_service.exception;

import org.springframework.http.HttpStatus;

/**
 * Exception thrown for business logic violations.
 */
public class BusinessException extends AppException {
    public BusinessException(String message) {
        super(message, HttpStatus.CONFLICT);
    }
}