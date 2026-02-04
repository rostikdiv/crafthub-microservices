package com.crafthub.user_service.entity.enums;

public enum VerificationStatus {
    PENDING,   // Очікує перевірки
    APPROVED,  // Підтверджено (для документа)
    REJECTED,  // Відхилено
    VERIFIED   // (Можна видалити, якщо не використовуєте, ми використовуємо APPROVED)
}