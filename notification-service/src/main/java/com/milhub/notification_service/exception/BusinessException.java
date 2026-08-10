package com.milhub.notification_service.exception;

import com.milhub.notification_service.exception.AppException;
import org.springframework.http.HttpStatus;

public class BusinessException extends AppException {
    public BusinessException(String message) {
        super(message, HttpStatus.BAD_REQUEST); // або 409 Conflict
    }
}