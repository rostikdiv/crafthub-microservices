package com.crafthub.cart_service.service;

import com.crafthub.cart_service.client.ProductServiceClient;
import com.crafthub.cart_service.dto.ProductResponseDTO;
import io.github.resilience4j.circuitbreaker.annotation.CircuitBreaker;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Slf4j
public class ProductServiceIntegration {

    private final ProductServiceClient productServiceClient;

    @CircuitBreaker(name = "productService", fallbackMethod = "getProductFallback")
    public ProductResponseDTO getProductById(UUID productId) {
        return productServiceClient.getProductById(productId);
    }
    @CircuitBreaker(name = "productService", fallbackMethod = "getProductsByIdsFallback")
    public List<ProductResponseDTO> getProductsByIds(List<UUID> productIds) {
        return productServiceClient.getProductsByIds(productIds);
    }

    // FALLBACK: Якщо сервіс товарів лежить, повертаємо null.
    // CartService обробить це, використавши старі дані з БД.
    public ProductResponseDTO getProductFallback(UUID productId, Throwable t) {
        log.warn("⚠️ Circuit Breaker: Failed to get product {}. Using local cache/DB data.", productId);
        return null;
    }

    public List<ProductResponseDTO> getProductsByIdsFallback(List<UUID> productIds, Throwable t) {
        log.warn("⚠️ Circuit Breaker: Failed to get products by IDs. Using local cache/DB data.", productIds);
        return null;
    }
}