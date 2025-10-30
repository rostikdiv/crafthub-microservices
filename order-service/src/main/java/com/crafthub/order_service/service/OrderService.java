package com.crafthub.order_service.service;

import com.crafthub.order_service.client.ProductServiceClient;
import com.crafthub.order_service.dto.OrderItemRequestDTO;
import com.crafthub.order_service.dto.OrderRequestDTO;
import com.crafthub.order_service.dto.ProductResponseDTO;
import com.crafthub.order_service.model.Order;
import com.crafthub.order_service.model.OrderItem;
import com.crafthub.order_service.repository.OrderRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Slf4j
public class OrderService {

    private final OrderRepository orderRepository;
    private final ProductServiceClient productServiceClient; // ❗️ Наш Feign-клієнт

    @Transactional
    public Order createOrder(OrderRequestDTO orderRequest, String userEmail) {
        log.info("Creating new order for user: {}", userEmail);

        // Створюємо замовлення
        Order order = new Order();
        order.setUserId(userEmail);
        order.setOrderNumber(UUID.randomUUID().toString());

        BigDecimal totalOrderPrice = BigDecimal.ZERO;

        // Обробляємо кожну позицію в замовленні
        for (OrderItemRequestDTO itemDto : orderRequest.items()) {

            // 1. СИНХРОННИЙ Дзвінок до product-service через Feign
            log.debug("Fetching product info for ID: {}", itemDto.productId());
            ProductResponseDTO product;
            try {
                product = productServiceClient.getProductById(itemDto.productId());
            } catch (Exception e) {
                // (Тут має бути обробка FeignException, наприклад 404)
                log.error("Error fetching product {}: {}", itemDto.productId(), e.getMessage());
                throw new RuntimeException("Product not found: " + itemDto.productId());
            }

            // 2. Валідація бізнес-логіки
            if (product.stockQuantity() < itemDto.quantity()) {
                log.warn("Insufficient stock for product {}: requested {}, available {}",
                        product.id(), itemDto.quantity(), product.stockQuantity());
                throw new RuntimeException("Insufficient stock for product: " + product.name());
            }

            // 3. Створюємо OrderItem
            OrderItem orderItem = OrderItem.builder()
                    .productId(product.id().toString())
                    .quantity(itemDto.quantity())
                    .pricePerUnit(product.price()) // ❗️ Беремо ціну з product-service!
                    .order(order) // Зв'язуємо з замовленням
                    .build();

            // 4. Додаємо ціну позиції до загальної суми
            totalOrderPrice = totalOrderPrice.add(
                    product.price().multiply(BigDecimal.valueOf(itemDto.quantity()))
            );

            // (Ми додамо оновлення залишків (product.setStockQuantity(...))
            // в наступній ітерації, це вимагає PUT-запиту)
        }

        // 5. Зберігаємо фінальні дані
        order.setTotalPrice(totalOrderPrice);
        // (Список orderItems буде збережено автоматично завдяки CascadeType.ALL)
        order.setOrderItems(order.getOrderItems()); // Це може бути необов'язково, залежно від реалізації

        Order savedOrder = orderRepository.save(order);
        log.info("Order {} created successfully for user {}", savedOrder.getOrderNumber(), userEmail);

        return savedOrder;
    }
}