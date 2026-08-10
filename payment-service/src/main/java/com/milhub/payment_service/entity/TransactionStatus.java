package com.milhub.payment_service.entity;

/**
 * Enumeration of possible states for a payment transaction.
 */
public enum TransactionStatus {
    PENDING,
    SUCCESS,
    FAILED,
    REFUNDED
}