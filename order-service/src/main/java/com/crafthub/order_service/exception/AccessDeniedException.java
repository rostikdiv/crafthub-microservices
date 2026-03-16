package com.crafthub.order_service.exception;

/**
 * Custom exception for access denial scenarios.
 */
public class AccessDeniedException extends RuntimeException {
    public AccessDeniedException(String message) {
        super(message);
    }
}