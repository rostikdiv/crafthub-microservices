package com.crafthub.order_service.service;

import com.crafthub.order_service.event.OrderCreatedEvent;
import com.crafthub.order_service.model.Order;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.awspring.cloud.sqs.operations.SqsTemplate;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
@Slf4j
@Profile("aws") // ❗️ Завантажувати, тільки якщо активний профіль "aws"
public class SqsPublisherService implements EventPublisherService {

    private final SqsTemplate sqsTemplate;
    private final ObjectMapper objectMapper;
    @org.springframework.beans.factory.annotation.Value("${application.sqs.orders-queue-url}")
    private String ordersQueueUrl;

    @Override
    public void publishOrderCreatedEvent(Order order) {
        try {
            OrderCreatedEvent event = new OrderCreatedEvent(
                    order.getOrderNumber(),
                    order.getUserId(),
                    order.getTotalPrice()
            );
            String message = objectMapper.writeValueAsString(event);
            // ❗️ Відправляємо в SQS
            sqsTemplate.send(ordersQueueUrl, message);
            log.info("Order Created event published to SQS: {}", message);
        } catch (Exception e) {
            log.error("Failed to publish OrderCreatedEvent to SQS: {}", e.getMessage());
        }
    }
}