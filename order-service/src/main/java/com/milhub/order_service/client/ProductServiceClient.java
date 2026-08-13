package com.milhub.order_service.client;

import com.milhub.order_service.dto.external.ProductResponseDTO;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;

import java.util.UUID;

/**
 * Feign client for the Product Service.
 * Provides endpoints for retrieving product information and managing stock.
 */
@FeignClient(name = "product-service", url = "${PRODUCT_SERVICE_URL:}", path = "/api/v1/products")
public interface ProductServiceClient {

    @GetMapping("/{id}")
    ProductResponseDTO getProductById(@PathVariable("id") UUID id);

    @PostMapping("/{id}/reduce-stock")
    void reduceStock(@PathVariable("id") UUID id, @RequestParam("quantity") Integer quantity);

    @PostMapping("/{id}/restore-stock")
    void restoreStock(@PathVariable("id") UUID id, @RequestParam("quantity") Integer quantity);
}