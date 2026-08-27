package com.milhub.order_service.entity.enums;

public enum ReturnStatus {
    PENDING, // Request submitted, awaiting delivery
    APPROVED, // Item received at warehouse, inspection passed
    REFUNDED, // Money refunded to customer
    REJECTED // Rejected (item damaged by customer, etc.)
}
