package com.milhub.delivery_service.controller;

import com.milhub.delivery_service.dto.request.ReturnShipmentRequestDTO;
import com.milhub.delivery_service.dto.response.ReturnShipmentResponseDTO;
import com.milhub.delivery_service.service.ShipmentService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/delivery/return")
@RequiredArgsConstructor
public class DeliveryReturnController {

    private final ShipmentService shipmentService;

    @PostMapping
    public ResponseEntity<ReturnShipmentResponseDTO> createReturnShipment(
            @RequestBody ReturnShipmentRequestDTO request) {
        return ResponseEntity.ok(shipmentService.createReturnShipment(request));
    }
}
