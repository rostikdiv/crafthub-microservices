package com.crafthub.delivery_service.entity;

public enum DeliveryStatus {
    PREPARING, // Paid, shipment draft created
    READY_TO_SHIP, // Packed by seller, waiting for courier/drop-off
    SHIPPED, // Handed over to carrier (tracking number assigned)
    DELIVERED, // Received by customer
    RETURNED, // Customer rejection or return
    CANCELLED // Delivery cancelled
}