package com.crafthub.cart_service.exception;

import lombok.Getter;
import org.springframework.http.HttpStatus;

/**
 * Base class for application-specific exceptions.
 */
@Getter
public class AppException extends RuntimeException {
    private final HttpStatus status;

    public AppException(String message, HttpStatus status) {
        super(message);
        this.status = status;
    }
}