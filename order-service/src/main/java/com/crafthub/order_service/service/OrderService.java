package com.crafthub.order_service.service;

import com.crafthub.order_service.client.PaymentServiceClient;
import com.crafthub.order_service.client.ProductServiceClient;
import com.crafthub.order_service.dto.delivery.DeliveryDetailsDTO;
import com.crafthub.order_service.dto.order.OrderItemRequestDTO;
import com.crafthub.order_service.dto.order.OrderItemResponseDTO;
import com.crafthub.order_service.dto.order.OrderRequestDTO;
import com.crafthub.order_service.dto.order.OrderResponseDTO;
import com.crafthub.order_service.dto.event.OrderPlacedEventDTO;
import com.crafthub.order_service.dto.external.ProductResponseDTO;
import com.crafthub.order_service.dto.payment.PaymentRequestDTO;
import com.crafthub.order_service.dto.payment.PaymentResponseDTO;
import com.crafthub.order_service.entity.Order;
import com.crafthub.order_service.entity.OrderItem;
import com.crafthub.order_service.entity.OrderStatus;
import com.crafthub.order_service.entity.enums.DeliveryType;
import com.crafthub.order_service.exception.AccessDeniedException;
import com.crafthub.order_service.repository.OrderRepository;
import com.crafthub.order_service.security.JwtParserService;
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
    private final PaymentServiceClient paymentServiceClient;
    private final JwtParserService jwtParserService;

    @Autowired(required = false) private KafkaPublisherService kafkaPublisherService;
    @Autowired(required = false) private SqsPublisherService sqsPublisherService;

    @Transactional
    public PaymentResponseDTO createOrder(OrderRequestDTO request) {
        // 1. Отримуємо токен та дані користувача
        String token = getTokenFromRequest();
        UUID userId = jwtParserService.extractUserId(token);
        String userRole = jwtParserService.extractUserRole(token);
        String userEmail = jwtParserService.extractUserEmail(token);
        boolean isVerified = jwtParserService.extractIsVerified(token);

        log.info("Creating order. User: {}, Email: {}, Role: {}", userId, userEmail, userRole);

        // 2. Базова перевірка прав
        if (!"BUYER".equals(userRole) && !"MILITARY_UNIT".equals(userRole)) {
            throw new AccessDeniedException("Only buyers or Military Units can place orders.");
        }

        // Створюємо заготовку замовлення (спочатку CREATED)
        validateDeliveryDetails(request.deliveryDetails());

        // Створюємо замовлення
        Order order = Order.builder()
                .userId(userId)
                .status(OrderStatus.CREATED)
                .items(new ArrayList<>())
                .deliveryInfo(request.deliveryDetails()) // ✅ Зберігаємо JSON Snapshot
                .build();

        BigDecimal totalOrderPrice = BigDecimal.ZERO;
        List<String> productNames = new ArrayList<>();

        // 3. Перевірка цін та наявності (ЗБЕРЕЖЕНА СТАРА ЛОГІКА)
        for (OrderItemRequestDTO itemRequest : request.items()) {

            // А. Робимо запит до Product Service
            ProductResponseDTO product = productServiceClient.getProductById(itemRequest.productId());

            // Б. Перевірка доступу (RESTRICTED)
            if ("RESTRICTED".equals(product.accessLevel())) {
                // Має бути військовим
                if (!"MILITARY_UNIT".equals(userRole)) {
                    throw new AccessDeniedException("Only Military Units can buy restricted products.");
                }
                // ✅ МАЄ БУТИ ВЕРИФІКОВАНИМ
                if (!isVerified) {
                    throw new AccessDeniedException("Your Military Unit account is not verified yet. Please upload documents.");
                }
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
        sendNotification(order, productNames, userEmail);

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
    public OrderResponseDTO getOrderById(UUID orderId) {
        Order order = orderRepository.findById(orderId)
                .orElseThrow(() -> new RuntimeException("Order not found"));
        return mapToOrderResponseDTO(order);
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
                itemsDto,
                order.getDeliveryInfo() // ✅ Передаємо адресу
        );
    }
    @Transactional
    public void updateOrderStatusFromDelivery(UUID orderId, String deliveryStatusStr) {
        Order order = orderRepository.findById(orderId)
                .orElseThrow(() -> new RuntimeException("Order not found: " + orderId));

        OrderStatus currentStatus = order.getStatus();
        OrderStatus newStatus = currentStatus; // За замовчуванням не змінюємо

        // Логіка мапінгу (згідно нашої таблиці)
        switch (deliveryStatusStr) {
            case "PREPARING":
                newStatus = OrderStatus.PREPARING;
                break;

            case "READY_TO_SHIP":
                // ⚡️ РОЗГАЛУЖЕННЯ: Якщо це самовивіз - то це "Готово до видачі"
                // Якщо пошта - то клієнту це знати рано, залишаємо PREPARING
                if (order.getDeliveryInfo().type() == DeliveryType.SELF_PICKUP) {
                    newStatus = OrderStatus.READY_FOR_PICKUP;
                } else {
                    newStatus = OrderStatus.PREPARING;
                }
                break;

            case "SHIPPED":
                newStatus = OrderStatus.SHIPPED;
                break;

            case "DELIVERED":
                newStatus = OrderStatus.DELIVERED;
                break;

            case "CANCELLED":
                newStatus = OrderStatus.CANCELLED;
                break;
        }

        // Оновлюємо тільки якщо статус реально змінився
        if (newStatus != currentStatus) {
            log.info("🔄 Updating Order {} status: {} -> {} (Delivery: {})",
                    orderId, currentStatus, newStatus, deliveryStatusStr);
            order.setStatus(newStatus);
            orderRepository.save(order);
        }
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
    private void validateDeliveryDetails(DeliveryDetailsDTO details) {
        if (details == null) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Delivery details are required");
        }
        if (details.provider() == null || details.type() == null) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Delivery provider and type are required");
        }

        // Перевірка залежно від типу
        if (details.type() == DeliveryType.BRANCH) {
            if (details.cityRef() == null || details.branchRef() == null) {
                throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "City and Branch are required for BRANCH delivery");
            }
        } else if (details.type() == DeliveryType.COURIER) {
            if (details.cityRef() == null || details.street() == null || details.building() == null) {
                throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Address details are required for COURIER delivery");
            }
        } else if (details.type() == DeliveryType.SELF_PICKUP) {
            // Для самовивозу бажано мати хоча б адресу точки текстом (snapshot)
            if (details.pickupAddress() == null) {
                throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Pickup address is required");
            }
        }
    }
}