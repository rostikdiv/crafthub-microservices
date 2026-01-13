package com.crafthub.delivery_service.client;

import com.crafthub.delivery_service.dto.external.OrderResponseDTO;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;

import java.util.UUID;

@FeignClient(name = "order-service", url = "${application.config.order-url:http://localhost:8083}") // Порт Order Service
public interface OrderServiceClient {

    @GetMapping("/api/v1/orders/{id}")
    OrderResponseDTO getOrderById(@PathVariable("id") UUID id);
}