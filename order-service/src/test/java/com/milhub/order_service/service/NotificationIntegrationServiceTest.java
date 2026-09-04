package com.milhub.order_service.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.milhub.order_service.entity.Order;
import com.milhub.order_service.entity.OutboxEvent;
import com.milhub.order_service.repository.OutboxEventRepository;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class NotificationIntegrationServiceTest {

    @Mock
    private OutboxEventRepository outboxEventRepository;

    @Mock
    private ObjectMapper objectMapper;

    @InjectMocks
    private NotificationIntegrationService notificationIntegrationService;

    @Test
    @DisplayName("publishOrderPlacedEvent saves event to Outbox successfully")
    void publishOrderPlacedEvent_ShouldSaveToOutbox() throws JsonProcessingException {
        Order order = new Order();
        order.setId(UUID.randomUUID());
        order.setUserId(UUID.randomUUID());
        order.setTotalPrice(BigDecimal.TEN);

        when(objectMapper.writeValueAsString(any())).thenReturn("{\"test\":\"json\"}");

        notificationIntegrationService.publishOrderPlacedEvent(order, List.of("Product1"), "test@test.com", List.of(UUID.randomUUID()));

        ArgumentCaptor<OutboxEvent> captor = ArgumentCaptor.forClass(OutboxEvent.class);
        verify(outboxEventRepository).save(captor.capture());

        OutboxEvent savedEvent = captor.getValue();
        assertThat(savedEvent.getAggregateId()).isEqualTo(order.getId().toString());
        assertThat(savedEvent.getEventType()).isEqualTo("OrderPlacedEvent");
        assertThat(savedEvent.getPayload()).isEqualTo("{\"test\":\"json\"}");
        assertThat(savedEvent.getStatus()).isEqualTo("PENDING");
    }

    @Test
    @DisplayName("publishOrderPlacedEvent throws RuntimeException on serialization failure")
    void publishOrderPlacedEvent_SerializationError() throws JsonProcessingException {
        Order order = new Order();
        order.setId(UUID.randomUUID());

        when(objectMapper.writeValueAsString(any())).thenThrow(new JsonProcessingException("Serialization failed") {});

        assertThrows(RuntimeException.class, () ->
                notificationIntegrationService.publishOrderPlacedEvent(order, List.of("Product1"), "test@test.com", List.of(UUID.randomUUID())));
    }

    @Test
    @DisplayName("publishRefundApprovedEvent saves event to Outbox successfully")
    void publishRefundApprovedEvent_ShouldSaveToOutbox() throws JsonProcessingException {
        UUID orderId = UUID.randomUUID();
        UUID productId = UUID.randomUUID();

        when(objectMapper.writeValueAsString(any())).thenReturn("{\"refund\":\"ok\"}");

        notificationIntegrationService.publishRefundApprovedEvent(orderId, productId, 2, "Faulty item");

        ArgumentCaptor<OutboxEvent> captor = ArgumentCaptor.forClass(OutboxEvent.class);
        verify(outboxEventRepository).save(captor.capture());

        OutboxEvent savedEvent = captor.getValue();
        assertThat(savedEvent.getAggregateId()).isEqualTo(orderId.toString());
        assertThat(savedEvent.getEventType()).isEqualTo("RefundApprovedEvent");
        assertThat(savedEvent.getPayload()).isEqualTo("{\"refund\":\"ok\"}");
        assertThat(savedEvent.getStatus()).isEqualTo("PENDING");
    }

    @Test
    @DisplayName("publishRefundApprovedEvent throws RuntimeException on serialization failure")
    void publishRefundApprovedEvent_SerializationError() throws JsonProcessingException {
        when(objectMapper.writeValueAsString(any())).thenThrow(new JsonProcessingException("Serialization error") {});

        assertThrows(RuntimeException.class, () ->
                notificationIntegrationService.publishRefundApprovedEvent(UUID.randomUUID(), UUID.randomUUID(), 1, "Reason"));
    }
}
