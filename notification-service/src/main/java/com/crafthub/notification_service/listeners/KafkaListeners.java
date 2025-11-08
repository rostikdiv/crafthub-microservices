package com.crafthub.notification_service.listeners;

import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Profile;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;

@Component
@Slf4j // Для логування (Lombok)
@Profile("kafka")
public class KafkaListeners {

    // ❗️ Головна логіка
    // Цей метод буде АВТОМАТИЧНО викликаний,
    // коли з'явиться нове повідомлення у топіку "orders_topic"
    @KafkaListener(
            topics = "orders_topic", // Назва "поштової скриньки"
            groupId = "notification-group" // Назва "команди" (з application.yml)
    )
    void handleOrderNotification(String message) {
        // У майбутньому ми б десеріалізували JSON з "message"
        // і отримали email та деталі замовлення.

        // Зараз ми просто симулюємо відправку email
        log.info("Отримано нове сповіщення про замовлення: {}", message);
        log.info("...симуляція відправки email...");

        // Тут могла б бути логіка для @Retryable,
        // якщо сервіс email недоступний
    }

    // Ти можеш додати більше методів @KafkaListener
    // для інших топіків (наприклад, "password_reset_topic")
}