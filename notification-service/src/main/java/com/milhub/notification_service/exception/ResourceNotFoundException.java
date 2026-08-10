package com.milhub.notification_service.exception;

import com.milhub.notification_service.exception.AppException;
import org.springframework.http.HttpStatus;

public class ResourceNotFoundException extends AppException {
    public ResourceNotFoundException(String message) {
        super(message, HttpStatus.NOT_FOUND);
    }
}