package com.crafthub.notification_service.listeners;

import com.crafthub.notification_service.dto.OrderPlacedEventDTO; // ✅
import com.crafthub.notification_service.service.EmailService; // ✅
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Profile;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;

@Component
@Slf4j
@Profile("local")
@RequiredArgsConstructor // ✅ Додаємо конструктор для ін'єкції сервісу
public class KafkaListeners {

    private final EmailService emailService; // ✅ Інжектимо сервіс

    @KafkaListener(
            topics = "order-placed-topic", // Перевір, чи тут правильний топік
            groupId = "notification-group",
            containerFactory = "kafkaListenerContainerFactory"
    )
    public void handleOrderNotification(OrderPlacedEventDTO event) { // ✅ Приймаємо DTO
        log.info("📨 Received Kafka event for Order ID: {}", event.orderId());

        // Викликаємо сервіс відправки
        emailService.sendOrderConfirmation(
                event.userEmail(),
                event.productName(),
                event.orderId().toString()
        );
    }
}