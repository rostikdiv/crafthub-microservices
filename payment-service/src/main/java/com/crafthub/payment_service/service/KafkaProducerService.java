package com.crafthub.payment_service.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Service;
import com.crafthub.payment_service.dto.payment.PaymentSuccessEventDTO;

/**
 * Service responsible for publishing payment-related events to Kafka.
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class KafkaProducerService {
    private final KafkaTemplate<String, PaymentSuccessEventDTO> kafkaTemplate;

    /**
     * Publishes a payment success event to the "payment-success-topic".
     *
     * @param event The event data containing order and payment details.
     */
    public void sendPaymentSuccessEvent(PaymentSuccessEventDTO event) {
        log.info("Sending payment success event for Order ID: {}", event.orderId());
        kafkaTemplate.send("payment-success-topic", event);
    }
}