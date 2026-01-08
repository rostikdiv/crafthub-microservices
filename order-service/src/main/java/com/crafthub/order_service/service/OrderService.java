package com.crafthub.order_service.service;

import com.crafthub.order_service.client.PaymentServiceClient;
import com.crafthub.order_service.client.ProductServiceClient;
import com.crafthub.order_service.dto.OrderItemRequestDTO;
import com.crafthub.order_service.dto.OrderItemResponseDTO;
import com.crafthub.order_service.dto.OrderRequestDTO;
import com.crafthub.order_service.dto.OrderResponseDTO;
import com.crafthub.order_service.dto.event.OrderPlacedEventDTO;
import com.crafthub.order_service.dto.external.ProductResponseDTO;
import com.crafthub.order_service.dto.payment.PaymentRequestDTO;
import com.crafthub.order_service.dto.payment.PaymentResponseDTO;
import com.crafthub.order_service.entity.Order;
import com.crafthub.order_service.entity.OrderItem;
import com.crafthub.order_service.entity.OrderStatus;
import com.crafthub.order_service.exception.AccessDeniedException;
import com.crafthub.order_service.repository.OrderRepository;
import com.crafthub.order_service.security.JwtParserService;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;
import org.springframework.web.server.ResponseStatusException;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Slf4j
public class OrderService {

    private final OrderRepository orderRepository;
    private final ProductServiceClient productServiceClient;
    private final PaymentServiceClient paymentServiceClient; // ✅ Новий клієнт
    private final JwtParserService jwtParserService;

    // Паблішери подій (Kafka/SQS)
    @Autowired(required = false) private KafkaPublisherService kafkaPublisherService;
    @Autowired(required = false) private SqsPublisherService sqsPublisherService;

    @Transactional
    public PaymentResponseDTO createOrder(OrderRequestDTO request) {
        // 1. Отримуємо токен та дані користувача
        String token = getTokenFromRequest();
        UUID userId = jwtParserService.extractUserId(token);
        String userRole = jwtParserService.extractUserRole(token);

        log.info("Creating order for User: {}, Role: {}", userId, userRole);

        // 2. Базова перевірка прав
        if (!"BUYER".equals(userRole) && !"MILITARY_UNIT".equals(userRole)) {
            throw new AccessDeniedException("Only buyers or Military Units can place orders.");
        }

        // Створюємо заготовку замовлення (спочатку CREATED)
        Order order = Order.builder()
                .userId(userId)
                .status(OrderStatus.CREATED)
                .items(new ArrayList<>())
                .build();

        BigDecimal totalOrderPrice = BigDecimal.ZERO;
        List<String> productNames = new ArrayList<>();

        // 3. Перевірка цін та наявності (ЗБЕРЕЖЕНА СТАРА ЛОГІКА)
        for (OrderItemRequestDTO itemRequest : request.items()) {

            // А. Робимо запит до Product Service
            ProductResponseDTO product = productServiceClient.getProductById(itemRequest.productId());

            // Б. Перевірка доступу (RESTRICTED)
            if ("RESTRICTED".equals(product.accessLevel()) && !"MILITARY_UNIT".equals(userRole)) {
                throw new AccessDeniedException("Access Denied: Product " + product.name() + " requires Military verification.");
            }

            // В. Перевірка залишків
            if (product.quantity() < itemRequest.quantity()) {
                throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Insufficient stock for product: " + product.name());
            }

            // Г. Розрахунок ціни
            BigDecimal itemTotal = product.price().multiply(BigDecimal.valueOf(itemRequest.quantity()));
            totalOrderPrice = totalOrderPrice.add(itemTotal);
            productNames.add(product.name());

            // Д. Створюємо OrderItem
            OrderItem orderItem = OrderItem.builder()
                    .productId(itemRequest.productId())
                    .quantity(itemRequest.quantity())
                    .pricePerUnit(product.price())
                    .order(order)
                    .build();

            order.getItems().add(orderItem);
        }

        order.setTotalPrice(totalOrderPrice);

        // 4. Зберігаємо замовлення
        orderRepository.save(order);
        log.info("✅ Order created with ID: {}", order.getId());

        // 5. Відправка повідомлення "Замовлення створено" (Як було раніше)
        sendNotification(order, productNames, "user@email.placeholder"); // Email можна спробувати дістати з токена, якщо там є

        // 6. 🚀 НОВА ЛОГІКА: Ініціація оплати
        log.info("Initiating payment for Order ID: {}", order.getId());

        PaymentRequestDTO paymentRequest = new PaymentRequestDTO(
                order.getId(),
                userId,
                totalOrderPrice
        );

        PaymentResponseDTO paymentResponse = paymentServiceClient.initPayment(paymentRequest);

        // Оновлюємо статус на PENDING_PAYMENT, бо ми отримали посилання
        order.setStatus(OrderStatus.PENDING_PAYMENT);
        orderRepository.save(order);

        // Повертаємо DTO з URL для оплати
        return paymentResponse;
    }

    // ✅ Метод для Kafka Listener (Коли оплата пройшла успішно)
    @Transactional
    public void confirmOrderPayment(UUID orderId) {
        log.info("💰 Payment confirmation received for Order: {}", orderId);

        Order order = orderRepository.findById(orderId)
                .orElseThrow(() -> new RuntimeException("Order not found: " + orderId));

        if (order.getStatus() == OrderStatus.PAID) {
            log.warn("Order {} is already PAID", orderId);
            return;
        }

        order.setStatus(OrderStatus.PAID);
        // Тут можна змінити статус на PREPARING, якщо оплата пройшла
        orderRepository.save(order);

        log.info("✅ Order {} status updated to PAID", orderId);
    }

    public List<OrderResponseDTO> getAllOrders() {
        return orderRepository.findAll().stream()
                .map(this::mapToOrderResponseDTO)
                .toList();
    }

    private OrderResponseDTO mapToOrderResponseDTO(Order order) {
        List<OrderItemResponseDTO> itemsDto = order.getItems().stream()
                .map(item -> new OrderItemResponseDTO(
                        item.getProductId(),
                        item.getQuantity(),
                        item.getPricePerUnit()
                ))
                .toList();

        return new OrderResponseDTO(
                order.getId(),
                order.getUserId(),
                order.getTotalPrice(),
                order.getStatus(),
                order.getCreatedAt(),
                itemsDto
        );
    }

    private void sendNotification(Order order, List<String> productNames, String userEmail) {
        String summary = String.join(", ", productNames);
        OrderPlacedEventDTO event = new OrderPlacedEventDTO(
                order.getId(), order.getUserId(), userEmail, summary, order.getTotalPrice()
        );

        if (kafkaPublisherService != null) kafkaPublisherService.sendOrderPlacedEvent(event);
        else if (sqsPublisherService != null) sqsPublisherService.sendOrderToQueue(event);
    }

    private String getTokenFromRequest() {
        var requestAttributes = (ServletRequestAttributes) RequestContextHolder.getRequestAttributes();
        if (requestAttributes == null) throw new ResponseStatusException(HttpStatus.UNAUTHORIZED);
        String authHeader = requestAttributes.getRequest().getHeader("Authorization");
        if (authHeader == null || !authHeader.startsWith("Bearer ")) throw new ResponseStatusException(HttpStatus.UNAUTHORIZED);
        return authHeader;
    }


}