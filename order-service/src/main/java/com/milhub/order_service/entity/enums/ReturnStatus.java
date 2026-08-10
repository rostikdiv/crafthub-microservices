package com.milhub.order_service.entity.enums;

public enum ReturnStatus {
    PENDING, // Заявку подано, очікує доставки
    APPROVED, // Товар отримано на складі, перевірка пройдена
    REFUNDED, // Гроші повернуто клієнту
    REJECTED // Відмовлено (товар пошкоджено клієнтом тощо)
}
