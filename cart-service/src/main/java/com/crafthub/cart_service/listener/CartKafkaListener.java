package com.crafthub.cart_service.listener;

import com.crafthub.cart_service.dto.OrderPlacedEventDTO;
import com.crafthub.cart_service.service.CartService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;

import java.util.UUID;

@Component
@Slf4j
@RequiredArgsConstructor
public class CartKafkaListener {

    private final CartService cartService;

    @KafkaListener(topics = "order-placed-topic", groupId = "cart-service-group")
    public void handleOrderPlaced(OrderPlacedEventDTO event) {
        log.info("🔔 Received OrderPlacedEvent: Order #{}", event.orderId());

        if (event.productIds() != null && !event.productIds().isEmpty()) {
            log.info("🛒 Cleaning up cart for user: {}. Removing {} items...", event.userId(), event.productIds().size());

            // Проходимось по кожному ID купленого товару і видаляємо його з кошика
            for (UUID productId : event.productIds()) {
                try {
                    cartService.removeItemFromCart(event.userId(), productId.toString());
                } catch (Exception e) {
                    log.error("⚠️ Failed to remove item {} from cart for user {}", productId, event.userId(), e);
                }
            }
            log.info("✅ Cart cleanup completed for user: {}", event.userId());
        } else {
            log.warn("⚠️ Received order event without product IDs. Skipping cart cleanup.");
        }
    }
}