package com.milhub.product_service.client;

import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;

import java.util.UUID;

/**
 * Feign client for interacting with the Order Service.
 */
@FeignClient(name = "order-service", url = "${ORDER_SERVICE_URL:https://milhub-order-service-258044247462.us-central1.run.app}") // Service name in Eureka
public interface OrderServiceClient {

    @GetMapping("/api/v1/orders/check-purchase")
    Boolean checkPurchase(@RequestParam("productId") UUID productId);
}