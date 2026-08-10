package com.milhub.order_service.service;

import com.milhub.order_service.client.ProductServiceClient;
import com.milhub.order_service.dto.external.ProductResponseDTO;
import com.milhub.order_service.dto.order.OrderItemRequestDTO;
import com.milhub.order_service.exception.BusinessException;
import io.github.resilience4j.circuitbreaker.annotation.CircuitBreaker;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.UUID;

/**
 * Integration service for interacting with the Product Service.
 * Implements resilience patterns using CircuitBreaker.
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class ProductIntegrationService {

    private final ProductServiceClient productServiceClient;

    /**
     * Retrieves product details from the Product Service.
     */
    @CircuitBreaker(name = "productService", fallbackMethod = "getProductFallback")
    public ProductResponseDTO getProductById(UUID productId) {
        return productServiceClient.getProductById(productId);
    }

    /**
     * Reduces the stock of a product in the Product Service.
     */
    @CircuitBreaker(name = "productService", fallbackMethod = "reduceStockFallback")
    public void reduceStock(UUID productId, Integer quantity) {
        productServiceClient.reduceStock(productId, quantity);
    }

    /**
     * Restores the stock of a product in the Product Service (Compensating
     * transaction).
     */
    @CircuitBreaker(name = "productService", fallbackMethod = "restoreStockFallback")
    public void restoreStock(UUID productId, Integer quantity) {
        productServiceClient.restoreStock(productId, quantity);
    }

    // ================= FALLBACK METHODS =================

    /**
     * Fallback method for getProductById.
     */
    public ProductResponseDTO getProductFallback(UUID productId, Throwable t) {
        log.warn("⚠️ Circuit Breaker: Could not get product {}. Reason: {}", productId, t.getMessage());
        throw new BusinessException("Product Service is temporarily unavailable. Please try again later.");
    }

    /**
     * Fallback method for reduceStock.
     */
    public void reduceStockFallback(UUID productId, Integer quantity, Throwable t) {
        log.warn("⚠️ Circuit Breaker: Could not reduce stock for {}. Reason: {}", productId, t.getMessage());
        throw new BusinessException("Failed to reserve product. Inventory service is unavailable.");
    }

    /**
     * Fallback method for restoreStock (Critical!).
     */
    public void restoreStockFallback(UUID productId, Integer quantity, Throwable t) {
        log.error("🚨 CRITICAL: Failed to ROLLBACK stock for product {}. Quantity: {}. Reason: {}",
                productId, quantity, t.getMessage());
        // TODO: Ideally send a "stock-release-retry" event to Kafka for later
        // processing
    }

    public void restoreStock(List<OrderItemRequestDTO> itemsToRestore) {
        // TODO Auto-generated method stub
        throw new UnsupportedOperationException("Unimplemented method 'restoreStock'");
    }
}