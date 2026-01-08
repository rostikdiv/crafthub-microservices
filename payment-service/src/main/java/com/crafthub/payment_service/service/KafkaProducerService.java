package com.crafthub.payment_service.service;

import com.crafthub.payment_service.dto.PaymentSuccessEventDTO;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
@Slf4j
public class KafkaProducerService {
    private final KafkaTemplate<String, PaymentSuccessEventDTO> kafkaTemplate;

    public void sendPaymentSuccessEvent(PaymentSuccessEventDTO event) {
        log.info("📤 Sending payment success event for Order ID: {}", event.orderId());
        kafkaTemplate.send("payment-success-topic", event);
    }
}