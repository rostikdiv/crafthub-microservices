package com.milhub.order_service.service;

import com.milhub.order_service.entity.OutboxEvent;
import com.milhub.order_service.repository.OutboxEventRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.PageRequest;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class OutboxSchedulerTest {

    @Mock
    private OutboxEventRepository outboxEventRepository;

    @Mock
    private KafkaPublisherService kafkaPublisherService;

    @InjectMocks
    private OutboxScheduler outboxScheduler;

    @org.junit.jupiter.api.BeforeEach
    void setUp() {
        org.springframework.test.util.ReflectionTestUtils.setField(outboxScheduler, "kafkaPublisherService", kafkaPublisherService);
    }

    @Test
    void processOutboxEvents_ShouldPublishAndMarkProcessed() {
        // Arrange
        OutboxEvent event = new OutboxEvent();
        event.setId(UUID.randomUUID());
        event.setAggregateId(UUID.randomUUID().toString());
        event.setEventType("OrderPlacedEvent");
        event.setPayload("{\"test\":\"data\"}");
        event.setStatus("PENDING");
        event.setCreatedAt(LocalDateTime.now());

        when(outboxEventRepository.findByStatusOrderByCreatedAtAsc(eq("PENDING"), any(PageRequest.class)))
                .thenReturn(List.of(event));

        // Act
        outboxScheduler.processOutboxEvents();

        // Assert
        verify(kafkaPublisherService).sendJsonEvent("order-placed-topic", event.getAggregateId(), event.getPayload());
        verify(outboxEventRepository).save(event);
        assert event.getStatus().equals("PROCESSED");
    }

    @Test
    void processOutboxEvents_WhenKafkaFails_ShouldRemainPending() {
        // Arrange
        OutboxEvent event = new OutboxEvent();
        event.setId(UUID.randomUUID());
        event.setAggregateId(UUID.randomUUID().toString());
        event.setEventType("OrderPlacedEvent");
        event.setPayload("{\"test\":\"data\"}");
        event.setStatus("PENDING");

        when(outboxEventRepository.findByStatusOrderByCreatedAtAsc(eq("PENDING"), any(PageRequest.class)))
                .thenReturn(List.of(event));

        doThrow(new RuntimeException("Kafka error")).when(kafkaPublisherService)
                .sendJsonEvent(anyString(), anyString(), anyString());

        // Act
        outboxScheduler.processOutboxEvents();

        // Assert
        verify(kafkaPublisherService).sendJsonEvent("order-placed-topic", event.getAggregateId(), event.getPayload());
        verify(outboxEventRepository, never()).save(any(OutboxEvent.class)); // Not saved as PROCESSED
        assert event.getStatus().equals("PENDING");
    }
}
