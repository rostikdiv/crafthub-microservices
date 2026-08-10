package com.milhub.product_service.exception;

import org.springframework.http.HttpStatus;

/**
 * Exception representing business logic conflicts.
 */
public class BusinessException extends AppException {
    public BusinessException(String message) {
        super(message, HttpStatus.CONFLICT); // 409 Conflict
    }
}