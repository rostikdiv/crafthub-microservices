package com.crafthub.delivery_service.exception;

import com.crafthub.delivery_service.exception.AppException;
import org.springframework.http.HttpStatus;

public class ResourceNotFoundException extends AppException {
    public ResourceNotFoundException(String message) {
        super(message, HttpStatus.NOT_FOUND);
    }
}