package com.milhub.delivery_service.service;

import com.milhub.delivery_service.client.OrderServiceClient;
import com.milhub.delivery_service.converter.LogisticsStatusMapper;
import com.milhub.delivery_service.dto.event.DeliveryStatusChangedEvent;
import com.milhub.delivery_service.dto.external.DeliveryDetailsDTO;
import com.milhub.delivery_service.dto.external.OrderResponseDTO;
import com.milhub.delivery_service.dto.request.ReturnShipmentRequestDTO;
import com.milhub.delivery_service.dto.response.ReturnShipmentResponseDTO;
import com.milhub.delivery_service.dto.response.ShipmentResponseDTO;
import com.milhub.delivery_service.entity.DeliveryProvider;
import com.milhub.delivery_service.entity.DeliveryStatus;
import com.milhub.delivery_service.entity.DeliveryType;
import com.milhub.delivery_service.entity.Shipment;
import com.milhub.delivery_service.entity.enums.ShipmentType;
import com.milhub.delivery_service.exception.BusinessException;
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

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
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
                .createdAt(LocalDateTime.now())
                .build();
    }

    // --- createShipment tests ---

    @Test
    void createShipment_WhenShipmentAlreadyExists_ShouldReturnEarly() {
        when(shipmentRepository.findByOrderId(orderId)).thenReturn(Optional.of(testShipment));

        shipmentService.createShipment(orderId);

        verify(orderServiceClient, never()).getOrderById(any());
        verify(shipmentRepository, never()).save(any());
    }

    @Test
    void createShipment_WhenOrderServiceThrowsException_ShouldThrowBusinessException() {
        when(shipmentRepository.findByOrderId(orderId)).thenReturn(Optional.empty());
        when(orderServiceClient.getOrderById(orderId)).thenThrow(new RuntimeException("Order service error"));

        BusinessException exception = assertThrows(
                BusinessException.class,
                () -> shipmentService.createShipment(orderId)
        );

        assertThat(exception.getMessage()).contains("Could not fetch order data for shipment creation");
        verify(shipmentRepository, never()).save(any());
    }

    @Test
    void createShipment_WhenValidWithProvider_ShouldSaveShipmentWithTrackingNumber() {
        when(shipmentRepository.findByOrderId(orderId)).thenReturn(Optional.empty());

        DeliveryDetailsDTO deliveryDetails = new DeliveryDetailsDTO(
                DeliveryProvider.NOVA_POSHTA,
                DeliveryType.BRANCH,
                "city-1", "Kyiv", "Kyivska",
                "branch-1", "Branch #1",
                null, null, null, null, null, null, null
        );
        OrderResponseDTO orderDTO = OrderResponseDTO.builder()
                .id(orderId)
                .userId(UUID.randomUUID())
                .deliveryInfo(deliveryDetails)
                .build();
        when(orderServiceClient.getOrderById(orderId)).thenReturn(orderDTO);

        shipmentService.createShipment(orderId);

        ArgumentCaptor<Shipment> shipmentCaptor = ArgumentCaptor.forClass(Shipment.class);
        verify(shipmentRepository).save(shipmentCaptor.capture());

        Shipment saved = shipmentCaptor.getValue();
        assertThat(saved.getOrderId()).isEqualTo(orderId);
        assertThat(saved.getStatus()).isEqualTo(DeliveryStatus.PREPARING);
        assertThat(saved.getDeliveryDetails()).isEqualTo(deliveryDetails);
        assertThat(saved.getTrackingNumber()).startsWith("NO-");
    }

    @Test
    void createShipment_WhenDeliveryInfoHasNoProvider_ShouldSaveShipmentWithoutTrackingNumber() {
        when(shipmentRepository.findByOrderId(orderId)).thenReturn(Optional.empty());

        DeliveryDetailsDTO deliveryDetails = new DeliveryDetailsDTO(
                null,
                DeliveryType.SELF_PICKUP,
                null, null, null,
                null, null,
                null, null, null, null, UUID.randomUUID(), "Pickup Address", "Notes"
        );
        OrderResponseDTO orderDTO = OrderResponseDTO.builder()
                .id(orderId)
                .userId(UUID.randomUUID())
                .deliveryInfo(deliveryDetails)
                .build();
        when(orderServiceClient.getOrderById(orderId)).thenReturn(orderDTO);

        shipmentService.createShipment(orderId);

        ArgumentCaptor<Shipment> shipmentCaptor = ArgumentCaptor.forClass(Shipment.class);
        verify(shipmentRepository).save(shipmentCaptor.capture());

        Shipment saved = shipmentCaptor.getValue();
        assertThat(saved.getOrderId()).isEqualTo(orderId);
        assertThat(saved.getTrackingNumber()).isNull();
    }

    // --- Query tests ---

    @Test
    void getAllShipments_ShouldReturnListOfDtos() {
        when(shipmentRepository.findAll()).thenReturn(List.of(testShipment));

        List<ShipmentResponseDTO> result = shipmentService.getAllShipments();

        assertThat(result).hasSize(1);
        assertThat(result.get(0).getId()).isEqualTo(shipmentId);
        assertThat(result.get(0).getTrackingNumber()).isEqualTo(trackingNumber);
    }

    @Test
    void getShipmentByOrderId_WhenFound_ShouldReturnDto() {
        when(shipmentRepository.findByOrderId(orderId)).thenReturn(Optional.of(testShipment));

        ShipmentResponseDTO result = shipmentService.getShipmentByOrderId(orderId);

        assertThat(result).isNotNull();
        assertThat(result.getId()).isEqualTo(shipmentId);
        assertThat(result.getOrderId()).isEqualTo(orderId);
    }

    @Test
    void getShipmentByOrderId_WhenNotFound_ShouldThrowResourceNotFoundException() {
        when(shipmentRepository.findByOrderId(orderId)).thenReturn(Optional.empty());

        assertThrows(ResourceNotFoundException.class, () -> shipmentService.getShipmentByOrderId(orderId));
    }

    // --- updateShipmentStatus tests ---

    @Test
    void updateShipmentStatus_WhenShipmentNotFound_ShouldThrowResourceNotFoundException() {
        when(shipmentRepository.findById(shipmentId)).thenReturn(Optional.empty());

        assertThrows(ResourceNotFoundException.class,
                () -> shipmentService.updateShipmentStatus(shipmentId, DeliveryStatus.DELIVERED));

        verify(shipmentRepository, never()).save(any());
        verify(kafkaTemplate, never()).send(any(), any(), any());
    }

    @Test
    void updateShipmentStatus_WhenStatusIsSame_ShouldReturnEarly() {
        when(shipmentRepository.findById(shipmentId)).thenReturn(Optional.of(testShipment));

        shipmentService.updateShipmentStatus(shipmentId, DeliveryStatus.PREPARING);

        verify(shipmentRepository, never()).save(any());
        verify(kafkaTemplate, never()).send(any(), any(), any());
    }

    @Test
    void updateShipmentStatus_WhenStatusChangedToShipped_ShouldSetShippedAtAndSendKafkaEvent() {
        when(shipmentRepository.findById(shipmentId)).thenReturn(Optional.of(testShipment));

        shipmentService.updateShipmentStatus(shipmentId, DeliveryStatus.SHIPPED);

        verify(shipmentRepository).save(testShipment);
        assertThat(testShipment.getStatus()).isEqualTo(DeliveryStatus.SHIPPED);
        assertThat(testShipment.getShippedAt()).isNotNull();

        verify(kafkaTemplate).send(eq("delivery-status-topic"), eq(orderId.toString()), any(DeliveryStatusChangedEvent.class));
    }

    @Test
    void updateShipmentStatus_WhenStatusChangedToDelivered_ShouldNotUpdateShippedAtAndSendKafkaEvent() {
        when(shipmentRepository.findById(shipmentId)).thenReturn(Optional.of(testShipment));

        shipmentService.updateShipmentStatus(shipmentId, DeliveryStatus.DELIVERED);

        verify(shipmentRepository).save(testShipment);
        assertThat(testShipment.getStatus()).isEqualTo(DeliveryStatus.DELIVERED);
        assertThat(testShipment.getShippedAt()).isNull();

        verify(kafkaTemplate).send(eq("delivery-status-topic"), eq(orderId.toString()), any(DeliveryStatusChangedEvent.class));
    }

    // --- return shipment tests ---

    @Test
    void createReturnShipment_WhenWeightBelowTwoKg_ShouldChargeBaseCost() {
        DeliveryDetailsDTO returnAddress = new DeliveryDetailsDTO(
                DeliveryProvider.NOVA_POSHTA, DeliveryType.BRANCH,
                "city-1", "Lviv", "Lvivska", "branch-2", "Branch #2",
                null, null, null, null, null, null, null
        );
        ReturnShipmentRequestDTO request = new ReturnShipmentRequestDTO(orderId, returnAddress, 1.5);

        ReturnShipmentResponseDTO response = shipmentService.createReturnShipment(request);

        assertThat(response).isNotNull();
        assertThat(response.trackingNumber()).startsWith("RET-");
        assertThat(response.shippingCost()).isEqualByComparingTo(BigDecimal.valueOf(70.0));

        ArgumentCaptor<Shipment> shipmentCaptor = ArgumentCaptor.forClass(Shipment.class);
        verify(shipmentRepository).save(shipmentCaptor.capture());
        Shipment saved = shipmentCaptor.getValue();
        assertThat(saved.getType()).isEqualTo(ShipmentType.RETURN);
        assertThat(saved.getStatus()).isEqualTo(DeliveryStatus.PREPARING);
    }

    @Test
    void createReturnShipment_WhenWeightAboveTwoKg_ShouldCalculateAdditionalCost() {
        DeliveryDetailsDTO returnAddress = new DeliveryDetailsDTO(
                DeliveryProvider.NOVA_POSHTA, DeliveryType.BRANCH,
                "city-1", "Lviv", "Lvivska", "branch-2", "Branch #2",
                null, null, null, null, null, null, null
        );
        ReturnShipmentRequestDTO request = new ReturnShipmentRequestDTO(orderId, returnAddress, 4.5);

        ReturnShipmentResponseDTO response = shipmentService.createReturnShipment(request);

        assertThat(response).isNotNull();
        // 70.0 + (4.5 - 2.0) * 10 = 70.0 + 25.0 = 95.0
        assertThat(response.shippingCost()).isEqualByComparingTo(BigDecimal.valueOf(95.0));
    }

    @Test
    void createReturnShipment_WhenWeightIsNull_ShouldChargeBaseCost() {
        DeliveryDetailsDTO returnAddress = new DeliveryDetailsDTO(
                DeliveryProvider.NOVA_POSHTA, DeliveryType.BRANCH,
                "city-1", "Lviv", "Lvivska", "branch-2", "Branch #2",
                null, null, null, null, null, null, null
        );
        ReturnShipmentRequestDTO request = new ReturnShipmentRequestDTO(orderId, returnAddress, null);

        ReturnShipmentResponseDTO response = shipmentService.createReturnShipment(request);

        assertThat(response).isNotNull();
        assertThat(response.shippingCost()).isEqualByComparingTo(BigDecimal.valueOf(70.0));
    }

    // --- Webhook tests ---

    @Test
    void processExternalWebhook_WhenValidStatus_ShouldUpdateStatusAndSendKafkaEvent() {
        String rawStatus = "Відправлення отримано";
        DeliveryProvider provider = DeliveryProvider.NOVA_POSHTA;

        when(shipmentRepository.findByTrackingNumber(trackingNumber)).thenReturn(Optional.of(testShipment));
        when(statusMapper.mapStatus(provider, rawStatus)).thenReturn(DeliveryStatus.DELIVERED);
        when(shipmentRepository.findById(shipmentId)).thenReturn(Optional.of(testShipment));

        shipmentService.processExternalWebhook(provider, trackingNumber, rawStatus);

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
        String rawStatus = "Unknown status from provider";
        DeliveryProvider provider = DeliveryProvider.NOVA_POSHTA;

        when(shipmentRepository.findByTrackingNumber(trackingNumber)).thenReturn(Optional.of(testShipment));
        when(statusMapper.mapStatus(provider, rawStatus)).thenReturn(null);

        shipmentService.processExternalWebhook(provider, trackingNumber, rawStatus);

        verify(shipmentRepository, never()).save(any());
        verify(kafkaTemplate, never()).send(any(), any(), any());
        assertThat(testShipment.getStatus()).isEqualTo(DeliveryStatus.PREPARING);
    }

    @Test
    void processExternalWebhook_WhenTrackingNumberNotFound_ShouldThrowException() {
        String rawStatus = "Відправлення отримано";
        DeliveryProvider provider = DeliveryProvider.NOVA_POSHTA;

        when(shipmentRepository.findByTrackingNumber(trackingNumber)).thenReturn(Optional.empty());

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
