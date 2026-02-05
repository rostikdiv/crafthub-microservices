package com.crafthub.order_service.entity;

public enum OrderStatus {
    // 🟡 Етап оформлення
    CREATED,
    PENDING_PAYMENT,

    // 🟢 Етап успіху
    PAID, // Гроші отримано
    PREPARING, // Комплектується (включає в себе і "Запаковано" для пошти)

    // 🔥 НОВИЙ СТАТУС (Тільки для самовивозу)
    READY_FOR_PICKUP, // Товар чекає в точці видачі

    SHIPPED, // Передано перевізнику (для пошти/кур'єра)
    DELIVERED, // Успішно отримано

    // 🔴 Етап скасування
    PAYMENT_FAILED,
    CANCELLED,
    REFUNDING,
    REFUNDED
}