package com.milhub.order_service.client;

import com.milhub.order_service.dto.delivery.ReturnShipmentRequestDTO;
import com.milhub.order_service.dto.delivery.ReturnShipmentResponseDTO;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;

/**
 * Feign client for the Delivery Service.
 */
@FeignClient(name = "delivery-service", path = "/api/v1/delivery/return")
public interface DeliveryServiceClient {

    @PostMapping
    ReturnShipmentResponseDTO createReturnShipment(@RequestBody ReturnShipmentRequestDTO request);
}
