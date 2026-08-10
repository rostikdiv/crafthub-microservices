package com.milhub.product_service.exception;

import lombok.Getter;
import org.springframework.http.HttpStatus;

/**
 * Base application exception class.
 */
@Getter
public class AppException extends RuntimeException {
    private final HttpStatus status;

    public AppException(String message, HttpStatus status) {
        super(message);
        this.status = status;
    }
}