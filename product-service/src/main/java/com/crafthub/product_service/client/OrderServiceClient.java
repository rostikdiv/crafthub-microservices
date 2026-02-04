package com.crafthub.product_service.client;

import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;

import java.util.UUID;

@FeignClient(name = "order-service") // Ім'я сервісу в Eureka
public interface OrderServiceClient {

    @GetMapping("/api/v1/orders/check-purchase")
    Boolean checkPurchase(@RequestParam("productId") UUID productId);
}