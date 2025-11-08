package com.crafthub.notification_service.listeners;

import io.awspring.cloud.sqs.annotation.SqsListener;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Component;

@Component
@Slf4j
@Profile("aws") // ❗️ Тільки для AWS
public class SqsListeners {

    // ❗️ Анотація SQS замість Kafka
    @SqsListener("orders_queue")
    public // Назва нашої SQS черги
    void handleOrderNotification(String message) {
        // Логіка та сама
        log.info("Отримано нове сповіщення про замовлення (з SQS): {}", message);
        log.info("...симуляція відправки email...");
    }
}