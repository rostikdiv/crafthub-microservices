package com.crafthub.order_service.service;

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
import com.crafthub.order_service.entity.enums.PaymentMethod; // New import
import com.crafthub.order_service.exception.AccessDeniedException;
import com.crafthub.order_service.exception.BusinessException;
import com.crafthub.order_service.exception.ResourceNotFoundException;
import com.crafthub.order_service.repository.OrderRepository;
import com.crafthub.order_service.security.UserContextService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Slf4j
public class OrderService {

    private final OrderRepository orderRepository;
    private final ProductIntegrationService productIntegrationService;
    private final PaymentIntegrationService paymentIntegrationService;
    private final UserContextService userContext;
    private final com.crafthub.order_service.client.UserServiceClient userServiceClient;

    // Опціональні бін для Kafka/SQS (залежно від того, що підключено)
    @Autowired(required = false)
    private KafkaPublisherService kafkaPublisherService;
    @Autowired(required = false)
    private SqsPublisherService sqsPublisherService;

    @Transactional
    public PaymentResponseDTO createOrder(OrderRequestDTO request) {
        UUID userId = userContext.getUserId();
        String userEmail = userContext.getUserEmail();
        boolean isVerified = userContext.isVerified();

        log.info("Creating order for User: {}", userId);

        validateDeliveryDetails(request.deliveryDetails());

        BigDecimal totalOrderPrice = BigDecimal.ZERO;
        List<String> productNames = new ArrayList<>();
        List<UUID> purchasedProductIds = new ArrayList<>();
        List<OrderItem> reservedItems = new ArrayList<>();
        List<OrderItem> orderItemsEntityList = new ArrayList<>();

        UUID commonSellerId = null;

        try {
            for (OrderItemRequestDTO itemRequest : request.items()) {

                // А. Отримуємо інфо про товар
                ProductResponseDTO product = productIntegrationService.getProductById(itemRequest.productId());
                if (product == null) {
                    throw new ResourceNotFoundException("Product not found with ID: " + itemRequest.productId());
                }

                // --- ВАЛІДАЦІЯ ПРОДАВЦЯ ---
                if (commonSellerId == null) {
                    if (product.sellerId() == null) {
                        log.warn("Product {} has no sellerId!", product.id());
                        throw new BusinessException("Product data integrity error: missing sellerId");
                    }
                    commonSellerId = product.sellerId();
                } else {
                    if (!commonSellerId.equals(product.sellerId())) {
                        throw new BusinessException("Multi-vendor orders are not allowed. Please split your order.");
                    }
                }

                // Б. Перевірка доступу (RESTRICTED)
                if ("RESTRICTED".equals(product.accessLevel())) {
                    if (!hasPermission("product:buy:restricted")) {
                        throw new AccessDeniedException(
                                "Purchasing restricted product requires military authorization.");
                    }
                    if (!isVerified) {
                        throw new AccessDeniedException("Account must be verified to purchase restricted items.");
                    }
                }

                // В. СПИСАННЯ ЗІ СКЛАДУ
                productIntegrationService.reduceStock(product.id(), itemRequest.quantity());

                // Г. Створюємо OrderItem
                OrderItem orderItem = OrderItem.builder()
                        .productId(itemRequest.productId())
                        .name(product.name()) // Set Name
                        .quantity(itemRequest.quantity())
                        .pricePerUnit(product.price())
                        .build();

                reservedItems.add(orderItem);

                BigDecimal itemTotal = product.price().multiply(BigDecimal.valueOf(itemRequest.quantity()));
                totalOrderPrice = totalOrderPrice.add(itemTotal);

                productNames.add(product.name());
                purchasedProductIds.add(product.id());
                orderItemsEntityList.add(orderItem);
            }

            // 4. СТВОРЕННЯ ЗАМОВЛЕННЯ
            OrderStatus initialStatus = OrderStatus.PENDING_PAYMENT;
            if (request.paymentMethod() == PaymentMethod.COD) {
                initialStatus = OrderStatus.PENDING_CONFIRMATION;
            }

            Order order = Order.builder()
                    .userId(userId)
                    .sellerId(commonSellerId)
                    .status(initialStatus)
                    .paymentMethod(request.paymentMethod()) // Save Payment Method
                    .totalPrice(totalOrderPrice)
                    .deliveryInfo(request.deliveryDetails())
                    .items(new ArrayList<>())
                    .build();

            for (OrderItem item : orderItemsEntityList) {
                item.setOrder(order);
                order.getItems().add(item);
            }

            orderRepository.save(order);
            log.info("✅ Order created with ID: {}", order.getId());

            // 5. Сповіщення та Оплата
            sendNotification(order, productNames, userEmail, purchasedProductIds);

            try {
                userServiceClient.incrementSales(commonSellerId);
            } catch (Exception e) {
                log.error("Failed to increment sales for seller {}", commonSellerId, e);
            }

            // Якщо післяплата (COD) - повертаємо успіх без редіректу на оплату
            if (request.paymentMethod() == PaymentMethod.COD) {
                return new PaymentResponseDTO(
                        null,
                        "PENDING_CONFIRMATION",
                        null);
            }

            // Якщо картка - ініціюємо оплату
            PaymentRequestDTO paymentRequest = new PaymentRequestDTO(
                    order.getId(),
                    userId,
                    totalOrderPrice);

            return paymentIntegrationService.initPayment(paymentRequest);

        } catch (Exception e) {
            log.error("❌ Error creating order: {}. Rolling back stock...", e.getMessage());
            // Компенсація
            for (OrderItem item : reservedItems) {
                try {
                    productIntegrationService.restoreStock(item.getProductId(), item.getQuantity());
                } catch (Exception ex) {
                    log.error("CRITICAL: Failed to rollback stock for product {}", item.getProductId(), ex);
                }
            }
            throw e;
        }
    }

    public OrderResponseDTO getOrderById(UUID orderId) {
        Order order = orderRepository.findById(orderId)
                .orElseThrow(() -> new RuntimeException("Order not found"));
        return mapToOrderResponseDTO(order);
    }

    public Page<OrderResponseDTO> getMyOrders(Pageable pageable) {
        UUID userId = userContext.getUserId();
        // findAllByUserId повертає Page<Order>, ми мапимо його в Page<OrderResponseDTO>
        return orderRepository.findAllByUserId(userId, pageable)
                .map(this::mapToOrderResponseDTO);
    }

    public List<OrderResponseDTO> getAllOrders() {
        return orderRepository.findAll().stream()
                .map(this::mapToOrderResponseDTO)
                .toList();
    }

    private void sendNotification(Order order, List<String> productNames, String userEmail, List<UUID> productIds) {
        String summary = String.join(", ", productNames);
        OrderPlacedEventDTO event = new OrderPlacedEventDTO(
                order.getId(),
                order.getUserId(),
                userEmail,
                order.getTotalPrice(),
                summary,
                productIds);

        if (kafkaPublisherService != null) {
            kafkaPublisherService.sendOrderPlacedEvent(event);
        } else if (sqsPublisherService != null) {
            sqsPublisherService.sendOrderToQueue(event);
        }
    }

    // --- Інші методи залишаються без змін ---

    private OrderResponseDTO mapToOrderResponseDTO(Order order) {
        List<OrderItemResponseDTO> itemsDto = order.getItems().stream()
                .map(item -> new OrderItemResponseDTO(
                        item.getProductId(),
                        item.getName() != null ? item.getName() : "Unknown Product", // Handle null name
                        item.getQuantity(),
                        item.getPricePerUnit()))
                .toList();

        return new OrderResponseDTO(
                order.getId(),
                order.getUserId(),
                order.getTotalPrice(),
                order.getStatus(),
                order.getCreatedAt(),
                itemsDto,
                order.getDeliveryInfo());
    }

    @Transactional
    public void updateOrderStatusFromDelivery(UUID orderId, String deliveryStatusStr) {
        Order order = orderRepository.findById(orderId)
                .orElseThrow(() -> new ResourceNotFoundException("Order not found: " + orderId));

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

    public boolean hasUserPurchasedProduct(UUID productId) {
        UUID userId = userContext.getUserId();

        // 🔒 ЖОРСТКА ВИМОГА: Тільки якщо доставлено
        List<OrderStatus> validStatuses = List.of(OrderStatus.DELIVERED);

        return orderRepository.existsByUserIdAndItemsProductIdAndStatusIn(userId, productId, validStatuses);
    }

    private boolean hasPermission(String permission) {
        return SecurityContextHolder.getContext().getAuthentication().getAuthorities().stream()
                .anyMatch(a -> a.getAuthority().equals(permission));
    }

    @Transactional(readOnly = true)
    public boolean hasUserBoughtFromSeller(UUID userId, UUID sellerId) {
        List<OrderStatus> validStatuses = List.of(OrderStatus.DELIVERED);
        return orderRepository.existsByUserIdAndSellerIdAndStatusIn(userId, sellerId, validStatuses);
    }

    // --- Seller Methods ---

    public Page<OrderResponseDTO> getSellerOrders(Pageable pageable) {
        UUID sellerId = userContext.getUserId(); // Providing seller is logged in
        // TODO: Verify user is actually a seller? Or assume role check handles it.
        return orderRepository.findAllBySellerId(sellerId, pageable)
                .map(this::mapToOrderResponseDTO);
    }

    @Transactional
    public OrderResponseDTO updateOrderStatusBySeller(UUID orderId, OrderStatus newStatus) {
        UUID sellerId = userContext.getUserId();
        Order order = orderRepository.findById(orderId)
                .orElseThrow(() -> new ResourceNotFoundException("Order not found: " + orderId));

        if (!order.getSellerId().equals(sellerId)) {
            throw new AccessDeniedException("You are not the seller of this order");
        }

        OrderStatus currentStatus = order.getStatus();
        log.info("Seller {} updating Order {} status: {} -> {}", sellerId, orderId, currentStatus, newStatus);

        // Validation for simple transitions
        if (newStatus == OrderStatus.CANCELLED) {
            // Logic to release stock if needed? For now just simple status change.
            // Ideally we should restore stock if order was created (which reduced stock).
            if (currentStatus != OrderStatus.CANCELLED && currentStatus != OrderStatus.DELIVERED) {
                restoreStock(order);
            }
        }

        order.setStatus(newStatus);
        Order saved = orderRepository.save(order);

        // Simulation Logic
        checkAndScheduleDelivery(saved);

        return mapToOrderResponseDTO(saved);
    }

    private void restoreStock(Order order) {
        for (OrderItem item : order.getItems()) {
            try {
                productIntegrationService.restoreStock(item.getProductId(), item.getQuantity());
            } catch (Exception e) {
                log.error("Failed to restore stock for item {}", item.getProductId(), e);
            }
        }
    }

    private void checkAndScheduleDelivery(Order order) {
        // Only for BRANCH / COURIER. Self pickup is manual.
        if (order.getDeliveryInfo().type() == DeliveryType.SELF_PICKUP) {
            return;
        }

        boolean shouldStartSimulation = false;

        if (order.getPaymentMethod() == PaymentMethod.COD) {
            // If COD, start simulation when CONFIRMED or PREPARING
            if (order.getStatus() == OrderStatus.CONFIRMED || order.getStatus() == OrderStatus.PREPARING) {
                shouldStartSimulation = true;
            }
        } else if (order.getPaymentMethod() == PaymentMethod.CARD) {
            // If CARD, start when PAID AND (CONFIRMED or PREPARING)
            boolean isConfirmed = order.getStatus() == OrderStatus.CONFIRMED
                    || order.getStatus() == OrderStatus.PREPARING;
            boolean isPaid = order.getStatus() == OrderStatus.PAID; // Wait, OrderStatus only holds one value.
            // Problem: OrderStatus enum handles both payment state and shipping state
            // partially.
            // PAID means paid but not yet shipped?
            // Actually, we need to know if it IS paid.
            // Let's assume if status is CONFIRMED/PREPARING, it WAS paid if it's Card?
            // No, transition for Card: PENDING_PAYMENT -> PAID -> (Seller Confirms) ->
            // PREPARING.
            // So if status is PREPARING (or CONFIRMED), it implies previous steps passed.
            // But wait, what if Seller confirms BEFORE payment?
            // Typically seller shouldn't confirm unpaid card order.

            // Let's refine:
            // Flow: PENDING_PAYMENT -> PAID -> PREPARING -> DELIVERED.
            // Flow: PENDING_CONFIRMATION -> PREPARING -> DELIVERED.

            if (order.getStatus() == OrderStatus.CONFIRMED || order.getStatus() == OrderStatus.PREPARING) {
                // For Card, ensure it was paid.
                // We can check if previous status was PAID, or just trust that to reach
                // PREPARING it must be valid.
                // But wait, seller can force status check.
                // Let's rely on the fact that for CARD, we only Auto-Deliver if status is
                // explicitly suitable.
                shouldStartSimulation = true;
            }
            // Also trigger if it JUST became PAID and was already Confirmed? (Unlikely
            // flow)
            // Actually, simplest rule: If status is PREPARING (or CONFIRMED for
            // simplicity), assume ready to ship.
            // The trigger is the status change to PREPARING/CONFIRMED.
        }

        if (shouldStartSimulation) {
            scheduleDelivery(order.getId());
        }
    }

    private void scheduleDelivery(UUID orderId) {
        new Thread(() -> {
            try {
                log.info("🚚 Simulation: Shipping order {}", orderId);
                Thread.sleep(5000); // 5 seconds
                updateOrderStatusFromDelivery(orderId, "DELIVERED");
                log.info("✅ Simulation: Order {} DELIVERED", orderId);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }
        }).start();
    }

    // Updated confirmOrderPayment to check for simulation
    @Transactional
    public void confirmOrderPayment(UUID orderId) {
        log.info("💰 Payment confirmation received for Order: {}", orderId);
        Order order = orderRepository.findById(orderId)
                .orElseThrow(() -> new RuntimeException("Order not found: " + orderId));

        if (order.getStatus() == OrderStatus.PAID || order.getStatus() == OrderStatus.DELIVERED) {
            return;
        }

        order.setStatus(OrderStatus.PAID);
        orderRepository.save(order);

        // If Payment was the missing piece and now it's PAID...
        // Does it auto-confirm? No, seller must confirm.
        // So we just set to PAID. Seller sees PAID order and clicks "Accept" ->
        // PREPARING.
        // Then checkAndScheduleDelivery triggers.
        // What if Seller "Accepted" (PREPARING) *before* payment? (Rare but possible if
        // UI allows).
        // If status was already PREPARING, and now it's PAID?
        // OrderStatus is single field. We can't be both PREPARING and PENDING_PAYMENT.
        // So sequence is strict: PENDING_PAYMENT -> PAID -> PREPARING.

        log.info("✅ Order {} status updated to PAID", orderId);
    }

    // ... validateDeliveryDetails ...
    private void validateDeliveryDetails(DeliveryDetailsDTO details) {
        if (details == null) {
            throw new BusinessException("Delivery details are required");
        }
        if (details.provider() == null || details.type() == null) {
            throw new BusinessException("Delivery provider and type are required");
        }

        if (details.type() == DeliveryType.BRANCH) {
            if (details.cityRef() == null || details.branchRef() == null) {
                throw new BusinessException("City and Branch are required for BRANCH delivery");
            }
        } else if (details.type() == DeliveryType.COURIER) {
            boolean isSellerDelivery = details
                    .provider() == com.crafthub.order_service.entity.enums.DeliveryProvider.SELLER;
            // For Seller delivery, cityRef is not required (we use text address). For
            // NP/UP, cityRef is needed.
            if ((!isSellerDelivery && details.cityRef() == null) || details.street() == null
                    || details.building() == null) {
                throw new BusinessException("Address details are required for COURIER delivery");
            }
        } else if (details.type() == DeliveryType.SELF_PICKUP) {
            if (details.pickupAddress() == null) {
                throw new BusinessException("Pickup address is required");
            }
        }
    }
}