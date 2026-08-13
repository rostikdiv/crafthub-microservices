package com.milhub.delivery_service.client;

import com.milhub.delivery_service.dto.external.OrderResponseDTO;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;

import java.util.UUID;

/**
 * Feign client for interacting with the Order Service.
 */
@FeignClient(name = "order-service", url = "${ORDER_SERVICE_URL:https://milhub-order-service-258044247462.us-central1.run.app}") // Order Service URL
public interface OrderServiceClient {

    @GetMapping("/api/v1/orders/{id}")
    OrderResponseDTO getOrderById(@PathVariable("id") UUID id);
}