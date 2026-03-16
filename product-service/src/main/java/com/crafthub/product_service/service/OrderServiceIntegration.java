package com.crafthub.product_service.service;

import com.crafthub.product_service.client.OrderServiceClient;
import io.github.resilience4j.circuitbreaker.annotation.CircuitBreaker;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.UUID;

/**
 * Integration service for interacting with the Order Service.
 * Implements a circuit breaker for resilience.
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class OrderServiceIntegration {

    private final OrderServiceClient orderServiceClient;

    /**
     * Checks if a specific product has been purchased by the current user.
     *
     * @param productId product identifier
     * @return true if purchased, false otherwise
     */
    @CircuitBreaker(name = "orderService", fallbackMethod = "checkPurchaseFallback")
    public boolean checkPurchase(UUID productId) {
        return orderServiceClient.checkPurchase(productId);
    }

    /**
     * Fallback method for checkPurchase in case of Order Service failure.
     * Denies the action by default for security.
     */
    public boolean checkPurchaseFallback(UUID productId, Throwable t) {
        log.warn("\u26a0\ufe0f Circuit Breaker: Failed to check purchase history. Reason: {}", t.getMessage());
        return false;
    }
}