package com.crafthub.user_service.entity.enums;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

@RequiredArgsConstructor
@Getter
public enum Permission {

    // --- ТОВАРИ (Product Service) ---
    PRODUCT_READ("product:read"),
    PRODUCT_CREATE("product:create"),
    PRODUCT_UPDATE("product:update"),
    PRODUCT_DELETE("product:delete"),

    // Право на купівлю специфічних товарів (тепловізори, плити)
    PRODUCT_BUY_RESTRICTED("product:buy:restricted"),

    // --- ЗАМОВЛЕННЯ (Order Service) ---
    ORDER_CREATE("order:create"),
    ORDER_READ_MY("order:read:my"),       // Покупець бачить свої
    ORDER_READ_ALL("order:read:all"),     // Адмін бачить все
    ORDER_UPDATE_STATUS("order:update:status"), // Продавець/Адмін міняє статус

    // --- ПРОФІЛЬ (User Service) ---
    PROFILE_UPDATE("profile:update"),

    // --- АДМІНІСТРУВАННЯ (Admin) ---
    USER_BAN("user:ban"),
    USER_VERIFY("user:verify"); // Підтвердження документів та верифікація юзера

    private final String permission;
}