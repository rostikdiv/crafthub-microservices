package com.crafthub.order_service.client;

import com.crafthub.order_service.dto.ProductResponseDTO;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;

// ❗️ name = "product-service"
// Це SERVICE_ID (spring.application.name) з Eureka,
// а не hostname!
@FeignClient(name = "product-service")
public interface ProductServiceClient {

    // ❗️ Цей шлях (@GetMapping) має *точно* // відповідати ендпоінту в ProductController
    @GetMapping("/api/v1/products/{id}")
    ProductResponseDTO getProductById(@PathVariable("id") String id);

    // (Пізніше ми додамо сюди метод для оновлення залишків на складі)
}