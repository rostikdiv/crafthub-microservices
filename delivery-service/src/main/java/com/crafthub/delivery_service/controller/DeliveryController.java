package com.crafthub.delivery_service.controller;

import com.crafthub.delivery_service.dto.response.ShipmentResponseDTO;
import com.crafthub.delivery_service.entity.DeliveryStatus;
import com.crafthub.delivery_service.service.ShipmentService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/delivery/shipments")
@RequiredArgsConstructor
public class DeliveryController {

    private final ShipmentService shipmentService;

    // Тільки адмін або кур'єр
    @GetMapping
    @PreAuthorize("hasAuthority('order:read:all')")
    public ResponseEntity<List<ShipmentResponseDTO>> getAllShipments() {
        return ResponseEntity.ok(shipmentService.getAllShipments());
    }

    // Доступно авторизованим (власник замовлення або адмін)
    @GetMapping("/orders/{orderId}")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<ShipmentResponseDTO> getByOrderId(@PathVariable UUID orderId) {
        return ResponseEntity.ok(shipmentService.getShipmentByOrderId(orderId));
    }

    // Зміна статусу: Продавець або Адмін
    @PatchMapping("/{id}/status")
    @PreAuthorize("hasAuthority('order:update:status')")
    public ResponseEntity<String> updateStatus(
            @PathVariable UUID id,
            @RequestParam DeliveryStatus newStatus
    ) {
        shipmentService.updateShipmentStatus(id, newStatus);
        return ResponseEntity.ok("Status updated to " + newStatus);
    }
}