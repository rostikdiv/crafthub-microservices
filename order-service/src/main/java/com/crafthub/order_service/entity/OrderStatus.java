package com.crafthub.order_service.entity;

/**
 * Enum representing the various statuses of an order through its lifecycle.
 */
public enum OrderStatus {
    //Creation Phase
    CREATED,
    PENDING_PAYMENT,
    PENDING_CONFIRMATION, // For Cash on Delivery (COD), awaiting seller confirmation
    CONFIRMED, // Confirmed by the seller
 
    //Fulfillment Phase
    PAID, // Payment received
    PREPARING, // Being picked and packed
 
    //NEW STATUS (Self-pickup only)
    READY_FOR_PICKUP, // Item is ready at the pickup point
 
    SHIPPED, // Handled over to carrier (post/courier)
    DELIVERED, // Successfully received by the customer
 
    // Cancellation Phase
    PAYMENT_FAILED,
    CANCELLED,
 
    // Returns Phase
    RETURN_REQUESTED,
    RETURN_APPROVED,
    RETURN_REJECTED,
 
    REFUNDING,
    REFUNDED
}