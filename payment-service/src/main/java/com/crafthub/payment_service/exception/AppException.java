package com.crafthub.payment_service.exception;

/**
 * Base exception for the Payment Service.
 */
public class AppException extends RuntimeException {
    public AppException(String message) {
        super(message);
    }
}