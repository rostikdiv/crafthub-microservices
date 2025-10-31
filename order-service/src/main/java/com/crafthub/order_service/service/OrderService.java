package com.crafthub.order_service.service;

import com.crafthub.order_service.client.ProductServiceClient;
import com.crafthub.order_service.dto.OrderItemRequestDTO;
import com.crafthub.order_service.dto.OrderRequestDTO;
import com.crafthub.order_service.dto.ProductResponseDTO;
import com.crafthub.order_service.model.Order;
import com.crafthub.order_service.model.OrderItem;
import com.crafthub.order_service.repository.OrderRepository;
import com.crafthub.order_service.event.OrderCreatedEvent; // ❗️ Імпорт класу події
import com.fasterxml.jackson.databind.ObjectMapper; // ❗️ Імпорт ObjectMapper
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.core.KafkaTemplate; // ❗️ Імпорт KafkaTemplate
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Slf4j
public class OrderService {

    private final OrderRepository orderRepository;
    private final ProductServiceClient productServiceClient;
    private final KafkaTemplate<String, String> kafkaTemplate; // ❗️ НОВЕ: Інжектуємо KafkaTemplate
    private final ObjectMapper objectMapper; // ❗️ НОВЕ: Інжектуємо ObjectMapper

    private static final String ORDERS_TOPIC = "orders_topic";

    @Transactional
    public Order createOrder(OrderRequestDTO orderRequest, String userEmail) {
        log.info("Creating new order for user: {}", userEmail);

        // ... (Код створення Order та OrderItem - без змін) ...
        Order order = new Order();
        order.setUserId(userEmail);
        order.setOrderNumber(UUID.randomUUID().toString());

        BigDecimal totalOrderPrice = BigDecimal.ZERO;

        // ... (логіка розрахунку товарів) ...

        // ... (Тут потрібно додати логіку обробки позицій)
        for (OrderItemRequestDTO itemDto : orderRequest.items()) {
            // ... (синхронний дзвінок Feign) ...
            ProductResponseDTO product;
            try {
                product = productServiceClient.getProductById(itemDto.productId());
            } catch (Exception e) {
                log.error("Error fetching product {}: {}", itemDto.productId(), e.getMessage());
                throw new RuntimeException("Product not found: " + itemDto.productId());
            }
            if (product.stockQuantity() < itemDto.quantity()) {
                log.warn("Insufficient stock for product {}: requested {}, available {}",
                        product.id(), itemDto.quantity(), product.stockQuantity());
                throw new RuntimeException("Insufficient stock for product: " + product.name());
            }

            OrderItem orderItem = OrderItem.builder()
                    .productId(product.id().toString())
                    .quantity(itemDto.quantity())
                    .pricePerUnit(product.price())
                    .order(order)
                    .build();

            totalOrderPrice = totalOrderPrice.add(
                    product.price().multiply(BigDecimal.valueOf(itemDto.quantity()))
            );
        }

        // 5. Зберігаємо фінальні дані
        order.setTotalPrice(totalOrderPrice);
        Order savedOrder = orderRepository.save(order);

        // 6. ❗️ НОВИЙ КРОК: Відправка асинхронної події в Kafka
        publishOrderCreatedEvent(savedOrder); // Викликаємо новий приватний метод

        log.info("Order {} created successfully for user {}", savedOrder.getOrderNumber(), userEmail);

        return savedOrder;
    }

    private void publishOrderCreatedEvent(Order order) {
        try {
            // 1. Створюємо DTO з потрібними даними
            OrderCreatedEvent event = new OrderCreatedEvent(
                    order.getOrderNumber(),
                    order.getUserId(),
                    order.getTotalPrice()
            );

            // 2. Серіалізуємо Java-об'єкт в JSON рядок
            String message = objectMapper.writeValueAsString(event);

            // 3. Відправляємо в Kafka
            kafkaTemplate.send(ORDERS_TOPIC, message);
            log.info("Order Created event published to Kafka: {}", message);
        } catch (Exception e) {
            log.error("Failed to publish OrderCreatedEvent for order {}: {}", order.getOrderNumber(), e.getMessage());
            // Важливо: Ми логуємо помилку, але не кидаємо її.
            // Замовлення вже в БД, Notification Service може повторити спробу пізніше.
        }
    }
}