package com.crafthub.product_service.exception;

import org.springframework.http.HttpStatus;

public class BusinessException extends AppException {
    public BusinessException(String message) {
        super(message, HttpStatus.CONFLICT); // 409 Conflict
    }
}