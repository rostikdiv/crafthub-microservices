package com.milhub.user_service.client;

import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;

import java.util.UUID;

/**
 * Feign client for communicating with the order-service.
 */
@FeignClient(name = "order-service", url = "${ORDER_SERVICE_URL:}", path = "/api/v1/orders")
public interface OrderServiceClient {

    /**
     * Checks if a specific buyer has a completed purchase from a specific seller.
     */
    @GetMapping("/check-seller-purchase")
    Boolean checkSellerPurchase(@RequestParam("userId") UUID userId,
            @RequestParam("sellerId") UUID sellerId);
}