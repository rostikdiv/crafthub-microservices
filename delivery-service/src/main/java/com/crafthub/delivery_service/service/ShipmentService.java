package com.crafthub.delivery_service.service;

import com.crafthub.delivery_service.client.OrderServiceClient;
import com.crafthub.delivery_service.dto.event.DeliveryStatusChangedEvent;
import com.crafthub.delivery_service.dto.external.OrderResponseDTO;
import com.crafthub.delivery_service.dto.response.ShipmentResponseDTO;
import com.crafthub.delivery_service.entity.DeliveryStatus;
import com.crafthub.delivery_service.entity.Shipment;
import com.crafthub.delivery_service.repository.ShipmentRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Slf4j
public class ShipmentService {

    private final ShipmentRepository shipmentRepository;
    private final OrderServiceClient orderServiceClient;

    private final KafkaTemplate<String, DeliveryStatusChangedEvent> kafkaTemplate;

    @Transactional
    public void createShipment(UUID orderId) {
        // 1. Перевірка на дублікати (ідемпотентність)
        if (shipmentRepository.findByOrderId(orderId).isPresent()) {
            log.warn("Shipment for order {} already exists", orderId);
            return;
        }

        log.info("Creating shipment for order: {}", orderId);

        // 2. Отримуємо деталі замовлення
        OrderResponseDTO orderData;
        try {
            orderData = orderServiceClient.getOrderById(orderId);
        } catch (Exception e) {
            log.error("Failed to fetch order data for ID: {}", orderId, e);
            throw new RuntimeException("Could not fetch order data");
        }

        // 3. Створюємо посилку
        Shipment shipment = Shipment.builder()
                .orderId(orderId)
                .status(DeliveryStatus.PREPARING)
                .deliveryDetails(orderData.getDeliveryInfo()) // Зберігаємо snapshot
                .build();

        // 4. (Опціонально) Тут можна одразу згенерувати фейковий ТТН
        if (orderData.getDeliveryInfo().provider() != null) {
            shipment.setTrackingNumber(generateFakeTrackingNumber(orderData.getDeliveryInfo().provider().name()));
        }

        shipmentRepository.save(shipment);
        log.info("✅ Shipment created successfully with ID: {}", shipment.getId());
    }

    @Transactional(readOnly = true)
    public List<ShipmentResponseDTO> getAllShipments() {
        return shipmentRepository.findAll().stream()
                .map(this::mapToDTO)
                .toList();
    }

    @Transactional(readOnly = true)
    public ShipmentResponseDTO getShipmentByOrderId(UUID orderId) {
        return shipmentRepository.findByOrderId(orderId)
                .map(this::mapToDTO)
                .orElseThrow(() -> new RuntimeException("Shipment not found for order: " + orderId));
    }

    @Transactional
    public void updateShipmentStatus(UUID shipmentId, DeliveryStatus newStatus) {
        Shipment shipment = shipmentRepository.findById(shipmentId)
                .orElseThrow(() -> new RuntimeException("Shipment not found"));

        if (shipment.getStatus() == newStatus) {
            return; // Статус не змінився, нічого не робимо
        }

        // 1. Оновлюємо БД
        shipment.setStatus(newStatus);
        if (newStatus == DeliveryStatus.SHIPPED) {
            shipment.setShippedAt(LocalDateTime.now());
        }
        shipmentRepository.save(shipment);

        // 2. Відправляємо подію в Kafka
        DeliveryStatusChangedEvent event = new DeliveryStatusChangedEvent(
                shipment.getOrderId(),
                newStatus,
                LocalDateTime.now()
        );

        log.info("📢 Sending DeliveryStatusChangedEvent: orderId={}, status={}", shipment.getOrderId(), newStatus);
        kafkaTemplate.send("delivery-status-topic", shipment.getOrderId().toString(), event);
    }

    // --- Mapper ---
    private ShipmentResponseDTO mapToDTO(Shipment shipment) {
        return ShipmentResponseDTO.builder()
                .id(shipment.getId())
                .orderId(shipment.getOrderId())
                .status(shipment.getStatus())
                .trackingNumber(shipment.getTrackingNumber())
                .deliveryDetails(shipment.getDeliveryDetails()) // Конвертер спрацює автоматично
                .createdAt(shipment.getCreatedAt())
                .shippedAt(shipment.getShippedAt())
                .build();
    }

    private String generateFakeTrackingNumber(String provider) {
        return provider.substring(0, 2) + "-" + System.currentTimeMillis();
    }
}