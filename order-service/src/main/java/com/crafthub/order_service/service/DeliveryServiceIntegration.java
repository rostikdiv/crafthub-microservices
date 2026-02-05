package com.crafthub.order_service.service;

import com.crafthub.order_service.client.DeliveryServiceClient;
import com.crafthub.order_service.dto.delivery.ReturnShipmentRequestDTO;
import com.crafthub.order_service.dto.delivery.ReturnShipmentResponseDTO;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
@Slf4j
public class DeliveryServiceIntegration {

    private final DeliveryServiceClient deliveryServiceClient;

    public ReturnShipmentResponseDTO createReturnShipment(ReturnShipmentRequestDTO request) {
        log.info("Requesting RETURN shipment from Delivery Service for Order: {}", request.orderId());
        try {
            return deliveryServiceClient.createReturnShipment(request);
        } catch (Exception e) {
            log.error("Failed to create return shipment", e);
            throw new RuntimeException("Failed to connect to Delivery Service", e);
        }
    }
}
