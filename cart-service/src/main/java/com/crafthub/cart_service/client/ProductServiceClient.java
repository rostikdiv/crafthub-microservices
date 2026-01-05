package com.crafthub.cart_service.client;

import com.crafthub.cart_service.dto.ProductResponseDTO; // ✅
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;

import java.util.List;
import java.util.UUID;

@FeignClient(name = "product-service")
public interface ProductServiceClient {

    @GetMapping("/api/v1/products/{id}")
    ProductResponseDTO getProductById(@PathVariable("id") UUID id); // ✅

    @PostMapping("/api/v1/products/batch")
    List<ProductResponseDTO> getProductsByIds(@RequestBody List<UUID> ids); // ✅
}