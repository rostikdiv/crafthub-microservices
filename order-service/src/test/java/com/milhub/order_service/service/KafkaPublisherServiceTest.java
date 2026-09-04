package com.milhub.order_service.service;

import com.milhub.order_service.dto.event.OrderPlacedEventDTO;
import com.milhub.order_service.dto.event.RefundApprovedEventDTO;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.kafka.core.KafkaTemplate;

import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class KafkaPublisherServiceTest {

    @Mock
    private KafkaTemplate<String, Object> kafkaTemplate;

    @InjectMocks
    private KafkaPublisherService kafkaPublisherService;

    @Test
    @DisplayName("sendOrderPlacedEvent successfully sends message to order-placed-topic")
    void sendOrderPlacedEvent_Success() {
        UUID orderId = UUID.randomUUID();
        OrderPlacedEventDTO event = new OrderPlacedEventDTO(
                orderId, UUID.randomUUID(), "buyer@milhub.com", BigDecimal.valueOf(500), "Helmet", List.of()
        );

        kafkaPublisherService.sendOrderPlacedEvent(event);

        verify(kafkaTemplate).send("order-placed-topic", orderId.toString(), event);
    }

    @Test
    @DisplayName("sendOrderPlacedEvent handles exception gracefully")
    void sendOrderPlacedEvent_ExceptionHandled() {
        UUID orderId = UUID.randomUUID();
        OrderPlacedEventDTO event = new OrderPlacedEventDTO(
                orderId, UUID.randomUUID(), "buyer@milhub.com", BigDecimal.valueOf(500), "Helmet", List.of()
        );
        doThrow(new RuntimeException("Kafka unreachable")).when(kafkaTemplate).send(any(), any(), any());

        assertDoesNotThrow(() -> kafkaPublisherService.sendOrderPlacedEvent(event));
    }

    @Test
    @DisplayName("sendRefundApprovedEvent successfully sends message to return-events topic")
    void sendRefundApprovedEvent_Success() {
        UUID orderId = UUID.randomUUID();
        RefundApprovedEventDTO event = new RefundApprovedEventDTO(orderId, UUID.randomUUID(), 1, "Damaged");

        kafkaPublisherService.sendRefundApprovedEvent(event);

        verify(kafkaTemplate).send("return-events", orderId.toString(), event);
    }

    @Test
    @DisplayName("sendRefundApprovedEvent handles exception gracefully")
    void sendRefundApprovedEvent_ExceptionHandled() {
        UUID orderId = UUID.randomUUID();
        RefundApprovedEventDTO event = new RefundApprovedEventDTO(orderId, UUID.randomUUID(), 1, "Damaged");
        doThrow(new RuntimeException("Kafka error")).when(kafkaTemplate).send(any(), any(), any());

        assertDoesNotThrow(() -> kafkaPublisherService.sendRefundApprovedEvent(event));
    }

    @Test
    @DisplayName("sendJsonEvent successfully sends raw JSON payload")
    void sendJsonEvent_Success() {
        kafkaPublisherService.sendJsonEvent("outbox-topic", "key-123", "{\"status\":\"ok\"}");

        verify(kafkaTemplate).send("outbox-topic", "key-123", "{\"status\":\"ok\"}");
    }

    @Test
    @DisplayName("sendJsonEvent re-throws RuntimeException when send fails")
    void sendJsonEvent_ThrowsException() {
        doThrow(new RuntimeException("Kafka failure")).when(kafkaTemplate).send(any(), any(), any());

        assertThrows(RuntimeException.class, () ->
                kafkaPublisherService.sendJsonEvent("outbox-topic", "key-123", "{\"status\":\"fail\"}"));
    }
}
