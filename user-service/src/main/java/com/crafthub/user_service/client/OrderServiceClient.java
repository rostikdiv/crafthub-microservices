package com.crafthub.user_service.client;

import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;

import java.util.UUID;

@FeignClient(name = "order-service", path = "/api/v1/orders")
public interface OrderServiceClient {

    @GetMapping("/check-seller-purchase")
    Boolean checkSellerPurchase(@RequestParam("userId") UUID userId,
                                @RequestParam("sellerId") UUID sellerId);
}