package com.crafthub.notification_service.listeners;

import com.crafthub.notification_service.dto.OrderPlacedEventDTO;
import com.crafthub.notification_service.service.EmailService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;

@Component
@Slf4j
@RequiredArgsConstructor
public class KafkaListeners {

    private final EmailService emailService;

    @KafkaListener(
            topics = "order-placed-topic",
            groupId = "notification-group",
            containerFactory = "kafkaListenerContainerFactory"
    )
    public void handleOrderNotification(OrderPlacedEventDTO event) {
        log.info("🔔 Kafka received: Order #{}", event.orderId());

        // Перевірка на всяк випадок, щоб не впало з NullPointerException
        String email = event.userEmail() != null ? event.userEmail() : "unknown@user.com";

        emailService.sendOrderConfirmation(
                email,
                event.orderId().toString(),
                event.totalPrice(),
                event.productName()
        );
    }
}