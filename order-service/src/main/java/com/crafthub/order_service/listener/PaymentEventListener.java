package com.crafthub.order_service.listener;

import com.crafthub.order_service.dto.event.PaymentSuccessEventDTO;
import com.crafthub.order_service.service.OrderService;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
@Slf4j
public class PaymentEventListener {

    private final OrderService orderService;
    private final ObjectMapper objectMapper;

    @KafkaListener(topics = "payment-success-topic", groupId = "order-service-group")
    public void handlePaymentSuccess(String message) {
        try {
            log.info("📨 Received payment success event: {}", message);
            PaymentSuccessEventDTO event = objectMapper.readValue(message, PaymentSuccessEventDTO.class);

            // Викликаємо сервіс для оновлення статусу
            orderService.confirmOrderPayment(event.orderId());

        } catch (Exception e) {
            log.error("❌ Error processing payment event", e);
        }
    }
}