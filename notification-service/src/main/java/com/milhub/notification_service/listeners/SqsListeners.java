package com.milhub.notification_service.listeners;

import io.awspring.cloud.sqs.annotation.SqsListener;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Component;

/**
 * Listener component for processing events received from AWS SQS queues.
 * Used as an alternative to Kafka when running in AWS environments.
 */
@Component
@Slf4j
@Profile("aws")
public class SqsListeners {

    /**
     * Handles order notifications received via SQS.
     */
    @SqsListener("${application.sqs.orders-queue-url}")
    public void handleOrderNotification(String message) {
        log.info("Received new order notification (from SQS): {}", message);
        log.info("Simulating email transmission...");
    }
}