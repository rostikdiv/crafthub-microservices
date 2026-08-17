package com.milhub.notification_service.listeners;

import com.milhub.notification_service.dto.DeliveryStatusChangedEvent;
import com.milhub.notification_service.dto.OrderPlacedEventDTO;
import com.milhub.notification_service.dto.PaymentSuccessEventDTO;
import com.milhub.notification_service.dto.UserVerificationEvent;
import com.milhub.notification_service.service.EmailService;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;

/**
 * Listener component for processing events received from Kafka topics.
 * Triggers corresponding email notifications based on the event type.
 */
@Component
@Slf4j
@RequiredArgsConstructor
public class KafkaListeners {

    private final EmailService emailService;
    private final ObjectMapper objectMapper;

    /**
     * Handles order-placed events.
     */
    @KafkaListener(topics = "order-placed-topic", groupId = "notification-group")
    public void handleOrderPlaced(String message) {
        try {
            OrderPlacedEventDTO event = parseMessage(message, OrderPlacedEventDTO.class);
            log.info("Notification: Order placed #{}", event.orderId());

            String email = event.userEmail() != null ? event.userEmail() : "unknown@user.com";
            emailService.sendOrderConfirmation(email, event.orderId().toString(), event.totalPrice(),
                    event.productName());
        } catch (Exception e) {
            log.error("Error processing order-placed event with payload: {}", message, e);
        }
    }

    /**
     * Handles payment-success events.
     */
    @KafkaListener(topics = "payment-success-topic", groupId = "notification-group")
    public void handlePaymentSuccess(String message) {
        try {
            PaymentSuccessEventDTO event = parseMessage(message, PaymentSuccessEventDTO.class);
            log.info("Notification: Payment success for order #{}", event.orderId());

            String email = event.userEmail() != null ? event.userEmail() : "user@example.com";
            emailService.sendPaymentSuccess(email, event.orderId().toString(), event.amount());
        } catch (Exception e) {
            log.error("Error processing payment-success event with payload: {}", message, e);
        }
    }

    /**
     * Handles delivery-status-topic events.
     */
    @KafkaListener(topics = "delivery-status-topic", groupId = "notification-group")
    public void handleDeliveryUpdate(String message) {
        try {
            DeliveryStatusChangedEvent event = parseMessage(message, DeliveryStatusChangedEvent.class);
            log.info("Notification: Delivery update for order #{} -> {}", event.orderId(), event.status());

            // Placeholder email; should be fetched from context or event
            emailService.sendDeliveryUpdate("user@example.com", event.orderId().toString(), event.status());
        } catch (Exception e) {
            log.error("Error processing delivery-status event with payload: {}", message, e);
        }
    }

    /**
     * Handles user-verification-topic events.
     */
    @KafkaListener(topics = "user-verification-topic", groupId = "notification-group")
    public void handleUserVerification(String message) {
        try {
            UserVerificationEvent event = parseMessage(message, UserVerificationEvent.class);
            log.info("Notification: Verification event for {} -> verified={}", event.email(), event.isVerified());

            if (event.isVerified()) {
                emailService.sendVerificationApproved(event.email());
            } else {
                emailService.sendVerificationRejected(event.email(), event.reason());
            }

        } catch (Exception e) {
            log.error("Error processing user-verification event with payload: {}", message, e);
        }
    }

    /**
     * Robust parser handling both raw JSON strings and double-encoded JSON strings.
     */
    private <T> T parseMessage(String rawMessage, Class<T> clazz) throws Exception {
        if (rawMessage == null || rawMessage.isBlank()) {
            throw new IllegalArgumentException("Empty message received");
        }
        String cleanJson = rawMessage.trim();
        if (cleanJson.startsWith("\"") && cleanJson.endsWith("\"") && cleanJson.length() > 2) {
            try {
                cleanJson = objectMapper.readValue(cleanJson, String.class);
            } catch (Exception ignored) {
            }
        }
        return objectMapper.readValue(cleanJson, clazz);
    }
}