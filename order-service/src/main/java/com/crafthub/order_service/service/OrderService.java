package com.crafthub.order_service.service;

import com.crafthub.order_service.client.ProductServiceClient;
import com.crafthub.order_service.dto.OrderRequestDTO;
import com.crafthub.order_service.dto.event.OrderPlacedEventDTO;
import com.crafthub.order_service.dto.external.ProductResponseDTO;
import com.crafthub.order_service.entity.Order;
import com.crafthub.order_service.exception.AccessDeniedException;
import com.crafthub.order_service.repository.OrderRepository;
import lombok.RequiredArgsConstructor; // Зверни увагу: ми приберемо final у паблішерів
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Slf4j
public class OrderService {

    private final OrderRepository orderRepository;
    private final ProductServiceClient productServiceClient;

    // 🔽 Інжекція з required = false (Якщо біна немає, буде null)

    @Autowired(required = false)
    private KafkaPublisherService kafkaPublisherService;

    @Autowired(required = false)
    private SqsPublisherService sqsPublisherService;

    @Transactional
    public String createOrder(OrderRequestDTO request, String userId, String userRole, String userEmail) {
        log.info("Processing order for User: {}, Role: {}", userId, userRole);

        // ... (код перевірки товару та розрахунку ціни той самий) ...
        ProductResponseDTO product = productServiceClient.getProductById(request.productId());

        if ("RESTRICTED".equals(product.accessLevel()) && !"MILITARY_UNIT".equals(userRole)) {
            throw new AccessDeniedException("Verification required.");
        }

        BigDecimal totalPrice = product.price().multiply(BigDecimal.valueOf(request.quantity()));

        Order order = Order.builder()
                .userId(UUID.fromString(userId))
                .productId(request.productId())
                .quantity(request.quantity())
                .totalPrice(totalPrice)
                .status("CREATED")
                .createdAt(LocalDateTime.now())
                .build();

        orderRepository.save(order);
        log.info("✅ Order saved: {}", order.getId());

        // 🔽 Логіка вибору паблішера
        OrderPlacedEventDTO event = new OrderPlacedEventDTO(
                order.getId(), order.getUserId(), userEmail, product.name(), order.getTotalPrice()
        );

        if (kafkaPublisherService != null) {
            kafkaPublisherService.sendOrderPlacedEvent(event);
        } else if (sqsPublisherService != null) {
            sqsPublisherService.sendOrderToQueue(event);
        } else {
            log.warn("⚠️ No Publisher Service available (neither Kafka nor SQS is active)");
        }

        return order.getId().toString();
    }
}