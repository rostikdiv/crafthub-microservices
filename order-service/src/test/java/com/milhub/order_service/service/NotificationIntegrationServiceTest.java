package com.milhub.order_service.service;

import com.milhub.order_service.entity.Order;
import com.milhub.order_service.entity.OutboxEvent;
import com.milhub.order_service.repository.OutboxEventRepository;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
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
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class NotificationIntegrationServiceTest {

    @Mock
    private OutboxEventRepository outboxEventRepository;

    @Mock
    private ObjectMapper objectMapper;

    @InjectMocks
    private NotificationIntegrationService notificationIntegrationService;

    @Test
    void publishOrderPlacedEvent_ShouldSaveToOutbox() throws JsonProcessingException {
        // Arrange
        Order order = new Order();
        order.setId(UUID.randomUUID());
        order.setUserId(UUID.randomUUID());
        order.setTotalPrice(BigDecimal.TEN);

        when(objectMapper.writeValueAsString(any())).thenReturn("{\"test\":\"json\"}");

        // Act
        notificationIntegrationService.publishOrderPlacedEvent(order, List.of("Product1"), "test@test.com", List.of(UUID.randomUUID()));

        // Assert
        ArgumentCaptor<OutboxEvent> captor = ArgumentCaptor.forClass(OutboxEvent.class);
        verify(outboxEventRepository).save(captor.capture());
        
        OutboxEvent savedEvent = captor.getValue();
        assertThat(savedEvent.getAggregateId()).isEqualTo(order.getId().toString());
        assertThat(savedEvent.getEventType()).isEqualTo("OrderPlacedEvent");
        assertThat(savedEvent.getPayload()).isEqualTo("{\"test\":\"json\"}");
        assertThat(savedEvent.getStatus()).isEqualTo("PENDING");
    }
}
