package com.crafthub.order_service.exception;

import lombok.Getter;
import org.springframework.http.HttpStatus;

/**
 * Base exception class for application-specific errors.
 */
@Getter
public class AppException extends RuntimeException {
    private final HttpStatus status;

    public AppException(String message, HttpStatus status) {
        super(message);
        this.status = status;
    }
}