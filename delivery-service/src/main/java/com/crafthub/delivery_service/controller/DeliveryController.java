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

    /**
     * Retrieves all shipments. Restricted to administrators or couriers.
     *
     * @return a list of all shipments
     */
    @GetMapping
    @PreAuthorize("hasAuthority('order:read:all')")
    public ResponseEntity<List<ShipmentResponseDTO>> getAllShipments() {
        return ResponseEntity.ok(shipmentService.getAllShipments());
    }

    /**
     * Retrieves shipment details for a specific order.
     * Accessible to authenticated users (order owners or administrators).
     *
     * @param orderId the unique identifier of the order
     * @return the shipment response details
     */
    @GetMapping("/orders/{orderId}")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<ShipmentResponseDTO> getByOrderId(@PathVariable UUID orderId) {
        return ResponseEntity.ok(shipmentService.getShipmentByOrderId(orderId));
    }

    /**
     * Updates the status of a shipment. Restricted to Sellers or Administrators.
     *
     * @param id        the unique identifier of the shipment
     * @param newStatus the new status to apply
     * @return a confirmation message
     */
    @PatchMapping("/{id}/status")
    @PreAuthorize("hasAuthority('order:update:status')")
    public ResponseEntity<String> updateStatus(
            @PathVariable UUID id,
            @RequestParam DeliveryStatus newStatus) {
        shipmentService.updateShipmentStatus(id, newStatus);
        return ResponseEntity.ok("Status updated to " + newStatus);
    }
}