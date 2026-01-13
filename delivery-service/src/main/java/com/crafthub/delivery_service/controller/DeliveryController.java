package com.crafthub.delivery_service.controller;

import com.crafthub.delivery_service.dto.response.ShipmentResponseDTO;
import com.crafthub.delivery_service.entity.DeliveryStatus;
import com.crafthub.delivery_service.service.ShipmentService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/delivery/shipments") // ✅ Змінили шлях на більш логічний
@RequiredArgsConstructor
public class DeliveryController {

    private final ShipmentService shipmentService;

    // 1. Отримати всі посилки (для адмінки)
    @GetMapping
    public ResponseEntity<List<ShipmentResponseDTO>> getAllShipments() {
        return ResponseEntity.ok(shipmentService.getAllShipments());
    }

    // 2. Знайти посилку по ID замовлення (для клієнта/Order Service)
    @GetMapping("/orders/{orderId}")
    public ResponseEntity<ShipmentResponseDTO> getByOrderId(@PathVariable UUID orderId) {
        return ResponseEntity.ok(shipmentService.getShipmentByOrderId(orderId));
    }

    @PatchMapping("/{id}/status")
    public ResponseEntity<String> updateStatus(
            @PathVariable UUID id,
            @RequestParam DeliveryStatus newStatus
    ) {
        shipmentService.updateShipmentStatus(id, newStatus);
        return ResponseEntity.ok("Status updated to " + newStatus);
    }

    @GetMapping("/health")
    public String healthCheck() {
        return "Delivery Service is operational";
    }
}