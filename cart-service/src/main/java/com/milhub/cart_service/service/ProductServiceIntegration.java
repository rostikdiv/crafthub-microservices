package com.milhub.cart_service.service;

import com.milhub.cart_service.dto.ProductResponseDTO;
import io.github.resilience4j.circuitbreaker.annotation.CircuitBreaker;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.UUID;

/**
 * Service for interacting with the Product Service.
 * Implements the Circuit Breaker pattern for resilience.
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class ProductServiceIntegration {

    private final com.milhub.cart_service.client.ProductServiceClient productServiceClient;

    /**
     * Retrieves a product by its unique ID.
     *
     * @param productId The UUID of the product.
     * @return The ProductResponseDTO or null if the service is unavailable.
     */
    @CircuitBreaker(name = "productService", fallbackMethod = "getProductFallback")
    public ProductResponseDTO getProductById(UUID productId) {
        return productServiceClient.getProductById(productId);
    }

    /**
     * Retrieves multiple products by their IDs in a single batch call.
     *
     * @param productIds List of product UUIDs.
     * @return A list of ProductResponseDTOs or null if the service is unavailable.
     */
    @CircuitBreaker(name = "productService", fallbackMethod = "getProductsByIdsFallback")
    public List<ProductResponseDTO> getProductsByIds(List<UUID> productIds) {
        return productServiceClient.getProductsByIds(productIds);
    }

    /**
     * Fallback method if the Product Service is unavailable.
     * Returns null to let CartService handle cached data.
     */
    public ProductResponseDTO getProductFallback(UUID productId, Throwable t) {
        log.warn("⚠️ Circuit Breaker: Failed to get product {}. Using local cache/DB data.", productId);
        return null;
    }

    /**
     * Fallback method for batch product retrieval.
     */
    public List<ProductResponseDTO> getProductsByIdsFallback(List<UUID> productIds, Throwable t) {
        log.warn("⚠️ Circuit Breaker: Failed to get products by IDs. Using local cache/DB data.", productIds);
        return null;
    }
}