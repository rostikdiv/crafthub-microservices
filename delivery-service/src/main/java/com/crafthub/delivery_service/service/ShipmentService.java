package com.crafthub.delivery_service.service;

import com.crafthub.delivery_service.client.OrderServiceClient;
import com.crafthub.delivery_service.dto.event.DeliveryStatusChangedEvent;
import com.crafthub.delivery_service.dto.external.OrderResponseDTO;
import com.crafthub.delivery_service.dto.response.ShipmentResponseDTO;
import com.crafthub.delivery_service.entity.DeliveryStatus;
import com.crafthub.delivery_service.entity.Shipment;
import com.crafthub.delivery_service.exception.BusinessException; // ✅
import com.crafthub.delivery_service.exception.ResourceNotFoundException; // ✅
import com.crafthub.delivery_service.repository.ShipmentRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

/**
 * Service for managing the lifecycle of shipments.
 * Handles shipment creation, status updates, and return shipments.
 * Integrates with Order Service and sends status change events via Kafka.
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class ShipmentService {

    private final ShipmentRepository shipmentRepository;
    private final OrderServiceClient orderServiceClient;
    private final KafkaTemplate<String, DeliveryStatusChangedEvent> kafkaTemplate;

    @Transactional
    public void createShipment(UUID orderId) {
        if (shipmentRepository.findByOrderId(orderId).isPresent()) {
            log.warn("Shipment for order {} already exists", orderId);
            return;
        }

        log.info("Creating shipment for order: {}", orderId);

        OrderResponseDTO orderData;
        try {
            orderData = orderServiceClient.getOrderById(orderId);
        } catch (Exception e) {
            log.error("Failed to fetch order data for ID: {}", orderId, e);
            // If order data cannot be retrieved, throw a business exception
            throw new BusinessException("Could not fetch order data for shipment creation");
        }

        Shipment shipment = Shipment.builder()
                .orderId(orderId)
                .status(DeliveryStatus.PREPARING)
                .deliveryDetails(orderData.getDeliveryInfo())
                .build();

        if (orderData.getDeliveryInfo().provider() != null) {
            shipment.setTrackingNumber(generateFakeTrackingNumber(orderData.getDeliveryInfo().provider().name()));
        }

        shipmentRepository.save(shipment);
        log.info("✅ Shipment created successfully with ID: {}", shipment.getId());

        // simulateDelivery(shipment.getId()); // Disable auto-simulation to allow
        // OrderService to control flow via Seller Confirmation
    }

    @Transactional(readOnly = true)
    public List<ShipmentResponseDTO> getAllShipments() {
        return shipmentRepository.findAll().stream().map(this::mapToDTO).toList();
    }

    @Transactional(readOnly = true)
    public ShipmentResponseDTO getShipmentByOrderId(UUID orderId) {
        return shipmentRepository.findByOrderId(orderId)
                .map(this::mapToDTO)
                .orElseThrow(() -> new ResourceNotFoundException("Shipment not found for order: " + orderId)); // ✅
    }

    @Transactional
    public void updateShipmentStatus(UUID shipmentId, DeliveryStatus newStatus) {
        Shipment shipment = shipmentRepository.findById(shipmentId)
                .orElseThrow(() -> new ResourceNotFoundException("Shipment not found")); // ✅

        if (shipment.getStatus() == newStatus)
            return;

        shipment.setStatus(newStatus);
        if (newStatus == DeliveryStatus.SHIPPED) {
            shipment.setShippedAt(LocalDateTime.now());
        }
        shipmentRepository.save(shipment);

        DeliveryStatusChangedEvent event = new DeliveryStatusChangedEvent(
                shipment.getOrderId(), newStatus, LocalDateTime.now());

        log.info("📢 Sending DeliveryStatusChangedEvent: orderId={}, status={}", shipment.getOrderId(), newStatus);
        kafkaTemplate.send("delivery-status-topic", shipment.getOrderId().toString(), event);
    }

    private ShipmentResponseDTO mapToDTO(Shipment shipment) {
        return ShipmentResponseDTO.builder()
                .id(shipment.getId())
                .orderId(shipment.getOrderId())
                .status(shipment.getStatus())
                .trackingNumber(shipment.getTrackingNumber())
                .deliveryDetails(shipment.getDeliveryDetails())
                .createdAt(shipment.getCreatedAt())
                .shippedAt(shipment.getShippedAt())
                .build();
    }

    private String generateFakeTrackingNumber(String provider) {
        return provider.substring(0, 2) + "-" + System.currentTimeMillis();
    }

    @Transactional
    public com.crafthub.delivery_service.dto.response.ReturnShipmentResponseDTO createReturnShipment(
            com.crafthub.delivery_service.dto.request.ReturnShipmentRequestDTO request) {
        log.info("Creating RETURN shipment for Order: {}", request.orderId());

        // 1. Create return shipment entity
        Shipment shipment = Shipment.builder()
                .orderId(request.orderId())
                .status(DeliveryStatus.PREPARING)
                .type(com.crafthub.delivery_service.entity.enums.ShipmentType.RETURN)
                .deliveryDetails(request.returnAddress())
                .trackingNumber("RET-" + System.currentTimeMillis())
                .build();

        shipmentRepository.save(shipment);
        log.info("✅ Return shipment created: {}", shipment.getId());

        // 2. Розрахунок вартості (Тимчасова логіка)
        java.math.BigDecimal shippingCost = java.math.BigDecimal.valueOf(70.0); // Базова ціна
        if (request.weight() != null && request.weight() > 2.0) {
            shippingCost = shippingCost.add(java.math.BigDecimal.valueOf((request.weight() - 2.0) * 10)); // +10 грн за
                                                                                                          // кожен кг
                                                                                                          // понад 2 кг
        }

        // 3. Start delivery simulation
        simulateDelivery(shipment.getId());

        return new com.crafthub.delivery_service.dto.response.ReturnShipmentResponseDTO(
                shipment.getId(),
                shipment.getTrackingNumber(),
                shippingCost);
    }

    private void simulateDelivery(UUID shipmentId) {
        java.util.concurrent.CompletableFuture.runAsync(() -> {
            try {
                Thread.sleep(5000); // 5 seconds delay
                log.info("🚚 Simulating delivery for shipment: {}", shipmentId);
                // Note: Calling this method directly bypasses @Transactional proxy,
                // but since repository methods are transactional, it handles the DB update
                // correctly for this simple case.
                updateShipmentStatus(shipmentId, DeliveryStatus.DELIVERED);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                log.warn("Delivery simulation interrupted for shipment: {}", shipmentId);
            } catch (Exception e) {
                log.error("Failed to simulate delivery for shipment: {}", shipmentId, e);
            }
        });
    }
}