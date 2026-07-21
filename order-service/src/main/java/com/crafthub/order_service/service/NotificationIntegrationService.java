package com.crafthub.order_service.service;

import com.crafthub.order_service.dto.event.OrderPlacedEventDTO;
import com.crafthub.order_service.dto.event.RefundApprovedEventDTO;
import com.crafthub.order_service.entity.Order;
import com.crafthub.order_service.entity.OutboxEvent;
import com.crafthub.order_service.repository.OutboxEventRepository;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Slf4j
public class NotificationIntegrationService {

    private final OutboxEventRepository outboxEventRepository;
    private final ObjectMapper objectMapper;

    public void publishOrderPlacedEvent(Order order, List<String> productNames, String userEmail, List<UUID> productIds) {
        String summary = String.join(", ", productNames);
        OrderPlacedEventDTO event = new OrderPlacedEventDTO(
                order.getId(),
                order.getUserId(),
                userEmail,
                order.getTotalPrice(),
                summary,
                productIds);

        try {
            String payload = objectMapper.writeValueAsString(event);
            OutboxEvent outboxEvent = OutboxEvent.builder()
                    .aggregateId(order.getId().toString())
                    .eventType("OrderPlacedEvent")
                    .payload(payload)
                    .status("PENDING")
                    .createdAt(LocalDateTime.now())
                    .build();
            
            outboxEventRepository.save(outboxEvent);
            log.info("Saved OrderPlacedEvent to Outbox for order: {}", order.getId());
        } catch (Exception e) {
            log.error("Failed to save OrderPlacedEvent to Outbox", e);
            throw new RuntimeException("Could not serialize event", e);
        }
    }

    public void publishRefundApprovedEvent(UUID orderId, UUID productId, Integer quantity, String reason) {
        RefundApprovedEventDTO event = new RefundApprovedEventDTO(orderId, productId, quantity, reason);
        try {
            String payload = objectMapper.writeValueAsString(event);
            OutboxEvent outboxEvent = OutboxEvent.builder()
                    .aggregateId(orderId.toString())
                    .eventType("RefundApprovedEvent")
                    .payload(payload)
                    .status("PENDING")
                    .createdAt(LocalDateTime.now())
                    .build();

            outboxEventRepository.save(outboxEvent);
            log.info("Saved RefundApprovedEvent to Outbox for order: {}", orderId);
        } catch (Exception e) {
            log.error("Failed to save RefundApprovedEvent to Outbox", e);
            throw new RuntimeException("Could not serialize event", e);
        }
    }
}
