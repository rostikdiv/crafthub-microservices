package com.crafthub.payment_service.exception;

import org.springframework.http.HttpStatus;

public class BusinessException extends AppException {
    public BusinessException(String message) {
        super(message, HttpStatus.CONFLICT);
    }
}