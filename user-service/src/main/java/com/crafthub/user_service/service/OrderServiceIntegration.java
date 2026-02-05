package com.crafthub.user_service.service;

import com.crafthub.user_service.client.OrderServiceClient;
import io.github.resilience4j.circuitbreaker.annotation.CircuitBreaker;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.UUID;

@Service
@RequiredArgsConstructor
@Slf4j
public class OrderServiceIntegration {

    private final OrderServiceClient orderServiceClient;

    @CircuitBreaker(name = "orderService", fallbackMethod = "checkSellerPurchaseFallback")
    public Boolean checkSellerPurchase(UUID userId, UUID sellerId) {
        return orderServiceClient.checkSellerPurchase(userId, sellerId);
    }

    // FALLBACK: Fail Safe
    // Якщо не можемо перевірити історію -> забороняємо дію (безпечніше для рейтингу)
    public Boolean checkSellerPurchaseFallback(UUID userId, UUID sellerId, Throwable t) {
        log.warn("⚠️ Circuit Breaker: Failed to check seller purchase verification. Reason: {}", t.getMessage());
        return false;
    }
}