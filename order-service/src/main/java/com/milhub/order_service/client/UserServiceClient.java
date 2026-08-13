package com.milhub.order_service.client;

import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;

import java.util.UUID;

/**
 * Feign client for the User Service (Seller management).
 */
@FeignClient(name = "user-service", url = "${USER_SERVICE_URL:https://milhub-user-service-258044247462.us-central1.run.app}", path = "/api/v1/sellers")
public interface UserServiceClient {

    @GetMapping("/{id}")
    com.milhub.order_service.dto.seller.SellerPublicProfileDTO getSellerProfile(@PathVariable("id") UUID id);

    @PostMapping("/internal/{id}/sales/increment")
    void incrementSales(@PathVariable("id") UUID id, @org.springframework.web.bind.annotation.RequestBody(required = false) String body);

    @GetMapping("/internal/{id}/auto-confirm")
    Boolean getAutoConfirm(@PathVariable("id") UUID id);
}
