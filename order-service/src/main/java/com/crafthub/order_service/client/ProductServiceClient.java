package com.crafthub.order_service.client;

import com.crafthub.order_service.dto.external.ProductResponseDTO;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;

import java.util.UUID;

// name: назва сервісу в Eureka
// path: базовий шлях API продуктів
@FeignClient(name = "product-service", path = "/api/v1/products")
public interface ProductServiceClient {

    @GetMapping("/{id}")
    ProductResponseDTO getProductById(@PathVariable("id") UUID id);
}