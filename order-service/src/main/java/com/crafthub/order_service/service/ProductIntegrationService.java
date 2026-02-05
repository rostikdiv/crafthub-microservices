package com.crafthub.order_service.service;

import com.crafthub.order_service.client.ProductServiceClient;
import com.crafthub.order_service.dto.external.ProductResponseDTO;
import com.crafthub.order_service.exception.BusinessException;
import io.github.resilience4j.circuitbreaker.annotation.CircuitBreaker;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.UUID;

@Service
@RequiredArgsConstructor
@Slf4j
public class ProductIntegrationService {

    private final ProductServiceClient productServiceClient;

    // --- 1. Отримання товару ---
    @CircuitBreaker(name = "productService", fallbackMethod = "getProductFallback")
    public ProductResponseDTO getProductById(UUID productId) {
        return productServiceClient.getProductById(productId);
    }

    // --- 2. Списання зі складу ---
    @CircuitBreaker(name = "productService", fallbackMethod = "reduceStockFallback")
    public void reduceStock(UUID productId, Integer quantity) {
        productServiceClient.reduceStock(productId, quantity);
    }

    // --- 3. Повернення на склад (Компенсація) ---
    @CircuitBreaker(name = "productService", fallbackMethod = "restoreStockFallback")
    public void restoreStock(UUID productId, Integer quantity) {
        productServiceClient.restoreStock(productId, quantity);
    }

    // ================= FALLBACK METHODS =================

    // Якщо не можемо отримати інфо про товар -> зупиняємо оформлення
    public ProductResponseDTO getProductFallback(UUID productId, Throwable t) {
        log.warn("⚠️ Circuit Breaker: Could not get product {}. Reason: {}", productId, t.getMessage());
        throw new BusinessException("Сервіс товарів тимчасово недоступний. Спробуйте пізніше.");
    }

    // Якщо не можемо списати товар -> зупиняємо оформлення
    public void reduceStockFallback(UUID productId, Integer quantity, Throwable t) {
        log.warn("⚠️ Circuit Breaker: Could not reduce stock for {}. Reason: {}", productId, t.getMessage());
        throw new BusinessException("Не вдалося зарезервувати товар. Сервіс складів недоступний.");
    }

    // Якщо не можемо повернути товар (Critical!) -> Логуємо для ручного втручання
    public void restoreStockFallback(UUID productId, Integer quantity, Throwable t) {
        log.error("🚨 CRITICAL: Failed to ROLLBACK stock for product {}. Quantity: {}. Reason: {}",
                productId, quantity, t.getMessage());
        // TODO: Тут в ідеалі треба відправити подію в Kafka "stock-release-retry", щоб спробувати пізніше
    }
}