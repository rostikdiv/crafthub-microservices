package com.milhub.delivery_service.service;

import com.milhub.delivery_service.client.OrderServiceClient;
import com.milhub.delivery_service.converter.LogisticsStatusMapper;
import com.milhub.delivery_service.dto.event.DeliveryStatusChangedEvent;
import com.milhub.delivery_service.entity.DeliveryProvider;
import com.milhub.delivery_service.entity.DeliveryStatus;
import com.milhub.delivery_service.entity.Shipment;
import com.milhub.delivery_service.exception.ResourceNotFoundException;
import com.milhub.delivery_service.repository.ShipmentRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.kafka.core.KafkaTemplate;

import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class ShipmentServiceTest {

    @Mock
    private ShipmentRepository shipmentRepository;

    @Mock
    private OrderServiceClient orderServiceClient;

    @Mock
    private KafkaTemplate<String, DeliveryStatusChangedEvent> kafkaTemplate;

    @Mock
    private LogisticsStatusMapper statusMapper;

    @InjectMocks
    private ShipmentService shipmentService;

    private Shipment testShipment;
    private final UUID shipmentId = UUID.randomUUID();
    private final UUID orderId = UUID.randomUUID();
    private final String trackingNumber = "20450000000001";

    @BeforeEach
    void setUp() {
        testShipment = Shipment.builder()
                .id(shipmentId)
                .orderId(orderId)
                .status(DeliveryStatus.PREPARING)
                .trackingNumber(trackingNumber)
                .build();
    }

    @Test
    void processExternalWebhook_WhenValidStatus_ShouldUpdateStatusAndSendKafkaEvent() {
        // Arrange
        String rawStatus = "Відправлення отримано";
        DeliveryProvider provider = DeliveryProvider.NOVA_POSHTA;

        when(shipmentRepository.findByTrackingNumber(trackingNumber)).thenReturn(Optional.of(testShipment));
        when(statusMapper.mapStatus(provider, rawStatus)).thenReturn(DeliveryStatus.DELIVERED);
        when(shipmentRepository.findById(shipmentId)).thenReturn(Optional.of(testShipment));

        // Act
        shipmentService.processExternalWebhook(provider, trackingNumber, rawStatus);

        // Assert
        verify(shipmentRepository).save(testShipment);
        assertThat(testShipment.getStatus()).isEqualTo(DeliveryStatus.DELIVERED);

        ArgumentCaptor<DeliveryStatusChangedEvent> eventCaptor = ArgumentCaptor.forClass(DeliveryStatusChangedEvent.class);
        verify(kafkaTemplate).send(eq("delivery-status-topic"), eq(orderId.toString()), eventCaptor.capture());

        DeliveryStatusChangedEvent capturedEvent = eventCaptor.getValue();
        assertThat(capturedEvent.getOrderId()).isEqualTo(orderId);
        assertThat(capturedEvent.getStatus()).isEqualTo(DeliveryStatus.DELIVERED);
    }

    @Test
    void processExternalWebhook_WhenStatusCannotBeMapped_ShouldNotUpdateShipment() {
        // Arrange
        String rawStatus = "Unknown status from provider";
        DeliveryProvider provider = DeliveryProvider.NOVA_POSHTA;

        when(shipmentRepository.findByTrackingNumber(trackingNumber)).thenReturn(Optional.of(testShipment));
        when(statusMapper.mapStatus(provider, rawStatus)).thenReturn(null);

        // Act
        shipmentService.processExternalWebhook(provider, trackingNumber, rawStatus);

        // Assert
        verify(shipmentRepository, never()).save(any());
        verify(kafkaTemplate, never()).send(any(), any(), any());
        assertThat(testShipment.getStatus()).isEqualTo(DeliveryStatus.PREPARING); // Status remains unchanged
    }

    @Test
    void processExternalWebhook_WhenTrackingNumberNotFound_ShouldThrowException() {
        // Arrange
        String rawStatus = "Відправлення отримано";
        DeliveryProvider provider = DeliveryProvider.NOVA_POSHTA;

        when(shipmentRepository.findByTrackingNumber(trackingNumber)).thenReturn(Optional.empty());

        // Act & Assert
        ResourceNotFoundException exception = assertThrows(
                ResourceNotFoundException.class,
                () -> shipmentService.processExternalWebhook(provider, trackingNumber, rawStatus)
        );

        assertThat(exception.getMessage()).contains("Shipment not found for tracking number: " + trackingNumber);
        verify(statusMapper, never()).mapStatus(any(), any());
        verify(shipmentRepository, never()).save(any());
        verify(kafkaTemplate, never()).send(anyString(), anyString(), any());
    }
}
