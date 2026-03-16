package com.crafthub.user_service.service;

import com.crafthub.user_service.client.OrderServiceClient;
import io.github.resilience4j.circuitbreaker.annotation.CircuitBreaker;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.UUID;

/**
 * Service for coordinating interactions with the Order Service.
 * Includes fault tolerance mechanisms like Circuit Breakers.
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class OrderServiceIntegration {

    private final OrderServiceClient orderServiceClient;

    /**
     * Verifies if a user has previously purchased from a specific seller.
     * Uses a circuit breaker to handle potential service outages.
     */
    @CircuitBreaker(name = "orderService", fallbackMethod = "checkSellerPurchaseFallback")
    public Boolean checkSellerPurchase(UUID userId, UUID sellerId) {
        return orderServiceClient.checkSellerPurchase(userId, sellerId);
    }

    /**
     * Fallback method for purchase verification.
     * Defaults to false (denying the action) if the Order Service is unreachable.
     */
    public Boolean checkSellerPurchaseFallback(UUID userId, UUID sellerId, Throwable t) {
        log.warn("⚠️ Circuit Breaker: Failed to check seller purchase verification. Reason: {}", t.getMessage());
        return false;
    }
}