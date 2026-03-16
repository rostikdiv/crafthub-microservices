package com.crafthub.delivery_service.exception;

import com.crafthub.delivery_service.exception.AppException;
import org.springframework.http.HttpStatus;

public class BusinessException extends AppException {
    public BusinessException(String message) {
        super(message, HttpStatus.BAD_REQUEST); // or 409 Conflict
    }
}