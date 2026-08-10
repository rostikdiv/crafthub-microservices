package com.milhub.cart_service.client;

import com.milhub.cart_service.dto.ProductResponseDTO;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;

import java.util.List;
import java.util.UUID;

/**
 * Feign client for the Product Service.
 */
@FeignClient(name = "product-service")
public interface ProductServiceClient {

    /**
     * Retrieves a single product by its ID.
     */
    @GetMapping("/api/v1/products/{id}")
    ProductResponseDTO getProductById(@PathVariable("id") UUID id);

    /**
     * Retrieves a batch of products by their IDs.
     */
    @PostMapping("/api/v1/products/batch")
    List<ProductResponseDTO> getProductsByIds(@RequestBody List<UUID> ids);
}