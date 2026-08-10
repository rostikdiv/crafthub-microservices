package com.milhub.product_service.entity.enums;

public enum AccessLevel {
    PUBLIC, // Accessible to everyone
    RESTRICTED // Only for military (Frontend hides UI, Order Service blocks purchase)
}