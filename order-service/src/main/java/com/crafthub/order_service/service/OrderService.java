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
import com.crafthub.order_service.security.UserContextService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.context.SecurityContextHolder;
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
    private final UserContextService userContext;

    // Опціональні бін для Kafka/SQS (залежно від того, що підключено)
    @Autowired(required = false) private KafkaPublisherService kafkaPublisherService;
    @Autowired(required = false) private SqsPublisherService sqsPublisherService;

    @Transactional
    public PaymentResponseDTO createOrder(OrderRequestDTO request) {
        // 1. Отримуємо дані користувача
        UUID userId = userContext.getUserId();
        String userRole = userContext.getUserRole();
        String userEmail = userContext.getUserEmail();
        boolean isVerified = userContext.isVerified();

        log.info("Creating order. User: {}, Role: {}", userId, userRole);

//        // 2. Валідація прав доступу
//        if (!"BUYER".equals(userRole) && !"MILITARY_UNIT".equals(userRole)) {
//            throw new AccessDeniedException("Only buyers or Military Units can place orders.");
//        }
        validateDeliveryDetails(request.deliveryDetails());

        // Підготовка змінних
        BigDecimal totalOrderPrice = BigDecimal.ZERO;
        List<String> productNames = new ArrayList<>();
        List<UUID> purchasedProductIds = new ArrayList<>();
        List<OrderItem> reservedItems = new ArrayList<>(); // Для відкату транзакції
        List<OrderItem> orderItemsEntityList = new ArrayList<>();

        UUID commonSellerId = null;

        try {
            // 3. 🔥 ЦИКЛ ОБРОБКИ ТОВАРІВ (Спочатку все перевіряємо і готуємо)
            for (OrderItemRequestDTO itemRequest : request.items()) {

                // А. Отримуємо інфо про товар (Тепер DTO містить sellerId!)
                ProductResponseDTO product = productServiceClient.getProductById(itemRequest.productId());

                // --- 🛑 ВАЛІДАЦІЯ ПРОДАВЦЯ ---
                if (commonSellerId == null) {
                    if (product.sellerId() == null) {
                        // Фолбек для старих товарів, де немає продавця (щоб не падало з NPE)
                        log.warn("Product {} has no sellerId!", product.id());
                        // Можна кинути помилку, або призначити "системного" продавця
                        throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR, "Product data integrity error: missing sellerId");
                    }
                    commonSellerId = product.sellerId();
                } else {
                    if (!commonSellerId.equals(product.sellerId())) {
                        throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                                "Multi-vendor orders are not allowed. Please split your order.");
                    }
                }

                // Б. Перевірка доступу (RESTRICTED)
                if ("RESTRICTED".equals(product.accessLevel())) {

                    // ❌ БУЛО (Hardcoded Role):
                    // if (!"MILITARY_UNIT".equals(userRole)) { ... }

                    // ✅ СТАЛО (Permission check):
                    // Перевіряємо, чи є у користувача дозвіл "product:buy:restricted"
                    if (!hasPermission("product:buy:restricted")) {
                        throw new AccessDeniedException("Purchasing this restricted product requires military authorization.");
                    }

                    // Додаткова перевірка верифікації (якщо треба)
                    if (!isVerified) {
                        throw new AccessDeniedException("Account must be verified to purchase restricted items.");
                    }
                }

                // В. ⚡️ СПИСАННЯ ЗІ СКЛАДУ
                productServiceClient.reduceStock(product.id(), itemRequest.quantity());

                // Г. Створюємо OrderItem (але поки без Order, бо Order ще не створений)
                OrderItem orderItem = OrderItem.builder()
                        .productId(itemRequest.productId())
                        .quantity(itemRequest.quantity())
                        .pricePerUnit(product.price())
                        .build();

                reservedItems.add(orderItem); // Зберігаємо для потенційного rollback

                // Д. Розрахунки
                BigDecimal itemTotal = product.price().multiply(BigDecimal.valueOf(itemRequest.quantity()));
                totalOrderPrice = totalOrderPrice.add(itemTotal);

                productNames.add(product.name());
                purchasedProductIds.add(product.id());

                orderItemsEntityList.add(orderItem);
            }

            // 4. ✅ СТВОРЕННЯ ТА ЗБЕРЕЖЕННЯ ЗАМОВЛЕННЯ
            // Тепер ми маємо sellerId і можемо створити валідний Order
            Order order = Order.builder()
                    .userId(userId)
                    .sellerId(commonSellerId) // Обов'язкове поле!
                    .status(OrderStatus.PENDING_PAYMENT) // Або CREATED, залежно від логіки
                    .totalPrice(totalOrderPrice)
                    .deliveryInfo(request.deliveryDetails()) // JSONB поле
                    .items(new ArrayList<>())
                    .build();

            // Прив'язуємо Items до Order (JPA зв'язок)
            for (OrderItem item : orderItemsEntityList) {
                item.setOrder(order);
                order.getItems().add(item);
            }

            // Зберігаємо (CascadeType.ALL збереже і айтеми)
            orderRepository.save(order);
            log.info("✅ Order created with ID: {}", order.getId());

            // 5. Сповіщення та Оплата
            sendNotification(order, productNames, userEmail, purchasedProductIds);

            PaymentRequestDTO paymentRequest = new PaymentRequestDTO(
                    order.getId(),
                    userId,
                    totalOrderPrice
            );

            PaymentResponseDTO paymentResponse = paymentServiceClient.initPayment(paymentRequest);

            // Оновлюємо статус, якщо ініціація успішна
            // (Хоча PENDING_PAYMENT ми вже поставили вище, це ок)

            return paymentResponse;

        } catch (Exception e) {
            log.error("❌ Error creating order: {}. Rolling back stock...", e.getMessage());

            // 🔄 КОМПЕНСАЦІЯ (ROLLBACK)
            for (OrderItem item : reservedItems) {
                try {
                    productServiceClient.restoreStock(item.getProductId(), item.getQuantity());
                } catch (Exception ex) {
                    log.error("CRITICAL: Failed to rollback stock for product {}", item.getProductId(), ex);
                }
            }
            throw e;
        }
    }

    private void sendNotification(Order order, List<String> productNames, String userEmail, List<UUID> productIds) {
        String summary = String.join(", ", productNames);
        OrderPlacedEventDTO event = new OrderPlacedEventDTO(
                order.getId(),
                order.getUserId(),
                userEmail,
                order.getTotalPrice(),
                summary,
                productIds
        );

        if (kafkaPublisherService != null) {
            kafkaPublisherService.sendOrderPlacedEvent(event);
        } else if (sqsPublisherService != null) {
            sqsPublisherService.sendOrderToQueue(event);
        }
    }

    // --- Інші методи залишаються без змін ---

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
                order.getDeliveryInfo()
        );
    }

    @Transactional
    public void updateOrderStatusFromDelivery(UUID orderId, String deliveryStatusStr) {
        Order order = orderRepository.findById(orderId)
                .orElseThrow(() -> new RuntimeException("Order not found: " + orderId));

        OrderStatus currentStatus = order.getStatus();
        OrderStatus newStatus = currentStatus;

        switch (deliveryStatusStr) {
            case "PREPARING":
                newStatus = OrderStatus.PREPARING;
                break;
            case "READY_TO_SHIP":
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

        if (newStatus != currentStatus) {
            log.info("🔄 Updating Order {} status: {} -> {} (Delivery: {})",
                    orderId, currentStatus, newStatus, deliveryStatusStr);
            order.setStatus(newStatus);
            orderRepository.save(order);
        }
    }

    private boolean hasPermission(String permission) {
        return SecurityContextHolder.getContext().getAuthentication().getAuthorities().stream()
                .anyMatch(a -> a.getAuthority().equals(permission));
    }

    private void validateDeliveryDetails(DeliveryDetailsDTO details) {
        if (details == null) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Delivery details are required");
        }
        if (details.provider() == null || details.type() == null) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Delivery provider and type are required");
        }

        if (details.type() == DeliveryType.BRANCH) {
            if (details.cityRef() == null || details.branchRef() == null) {
                throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "City and Branch are required for BRANCH delivery");
            }
        } else if (details.type() == DeliveryType.COURIER) {
            if (details.cityRef() == null || details.street() == null || details.building() == null) {
                throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Address details are required for COURIER delivery");
            }
        } else if (details.type() == DeliveryType.SELF_PICKUP) {
            if (details.pickupAddress() == null) {
                throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Pickup address is required");
            }
        }
    }
}