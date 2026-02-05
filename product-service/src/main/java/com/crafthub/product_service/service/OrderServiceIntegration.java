package com.crafthub.product_service.service;

import com.crafthub.product_service.client.OrderServiceClient;
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

    @CircuitBreaker(name = "orderService", fallbackMethod = "checkPurchaseFallback")
    public boolean checkPurchase(UUID productId) {
        return orderServiceClient.checkPurchase(productId);
    }

    // FALLBACK: Якщо не можемо перевірити -> забороняємо дію
    public boolean checkPurchaseFallback(UUID productId, Throwable t) {
        log.warn("⚠️ Circuit Breaker: Failed to check purchase history. Reason: {}", t.getMessage());
        return false;
    }
}