package com.crafthub.payment_service.exception;

/**
 * Exception thrown when a payment-related resource is not found.
 */
public class ResourceNotFoundException extends RuntimeException {
    public ResourceNotFoundException(String message) {
        super(message);
    }
}