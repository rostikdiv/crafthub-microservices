package com.crafthub.product_service.client;

import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import com.crafthub.product_service.dto.SellerInfoDTO;

import java.util.UUID;

/**
 * Feign client for interacting with the User Service.
 */
// Service name must match spring.application.name in
// user_service/application.yaml
@FeignClient(name = "user-service", path = "/api/v1/users")
public interface UserServiceClient {

    @GetMapping("/{userId}/seller-info")
    SellerInfoDTO getSellerInfo(@PathVariable("userId") UUID userId);
}