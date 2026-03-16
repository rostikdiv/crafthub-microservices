package com.crafthub.user_service.entity.enums;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

/**
 * Enum defining specific atomic permissions within the system for fine-grained
 * access control.
 */
@RequiredArgsConstructor
@Getter
public enum Permission {

    // --- Products (Product Service) ---
    PRODUCT_READ("product:read"),
    PRODUCT_CREATE("product:create"),
    PRODUCT_UPDATE("product:update"),
    PRODUCT_DELETE("product:delete"),

    // Permission to purchase restricted goods (e.g., thermal imagers)
    PRODUCT_BUY_RESTRICTED("product:buy:restricted"),

    // --- Orders (Order Service) ---
    ORDER_CREATE("order:create"),
    ORDER_READ_MY("order:read:my"), // Buyer sees their own
    ORDER_READ_ALL("order:read:all"), // Admin sees everything
    ORDER_UPDATE_STATUS("order:update:status"), // Seller/Admin changes status

    // --- Profile (User Service) ---
    PROFILE_UPDATE("profile:update"),

    // --- Administration (Admin) ---
    USER_BAN("user:ban"),
    USER_VERIFY("user:verify"); // Document confirmation and user verification

    private final String permission;
}