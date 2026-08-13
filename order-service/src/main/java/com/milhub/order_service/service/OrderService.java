package com.milhub.order_service.service;

import com.milhub.order_service.dto.delivery.DeliveryDetailsDTO;
import com.milhub.order_service.dto.order.OrderItemRequestDTO;
import com.milhub.order_service.dto.order.OrderItemResponseDTO;
import com.milhub.order_service.dto.order.OrderRequestDTO;
import com.milhub.order_service.dto.order.OrderResponseDTO;
import com.milhub.order_service.dto.event.OrderPlacedEventDTO;
import com.milhub.order_service.dto.external.ProductResponseDTO;
import com.milhub.order_service.dto.payment.PaymentRequestDTO;
import com.milhub.order_service.dto.payment.PaymentResponseDTO;
import com.milhub.order_service.entity.Order;
import com.milhub.order_service.entity.OrderItem;
import com.milhub.order_service.entity.OrderStatus;
import com.milhub.order_service.entity.enums.DeliveryType;
import com.milhub.order_service.entity.enums.PaymentMethod; // New import
import com.milhub.order_service.exception.AccessDeniedException;
import com.milhub.order_service.exception.BusinessException;
import com.milhub.order_service.exception.ResourceNotFoundException;
import com.milhub.order_service.repository.OrderRepository;
import com.milhub.order_service.security.UserContextService;
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

/**
 * Core service for managing orders, including creation, stock reduction,
 * payment initiation, and status management.
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class OrderService {

    private final OrderRepository orderRepository;
    private final ProductIntegrationService productIntegrationService;
    private final PaymentIntegrationService paymentIntegrationService;
    private final UserContextService userContext;
    private final com.milhub.order_service.client.UserServiceClient userServiceClient;
    private final NotificationIntegrationService notificationIntegrationService;
    private final InventoryIntegrationService inventoryIntegrationService;
    private final java.util.List<com.milhub.order_service.service.strategy.OrderStatusStrategy> statusStrategies;
    private final com.milhub.order_service.service.strategy.DefaultOrderStatusStrategy defaultStatusStrategy;

    @org.springframework.context.annotation.Lazy
    @Autowired
    private OrderService self;

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

                // A. Retrieve product information
                ProductResponseDTO product = productIntegrationService.getProductById(itemRequest.productId());
                if (product == null) {
                    throw new ResourceNotFoundException("Product not found with ID: " + itemRequest.productId());
                }

                // --- SELLER VALIDATION ---
                if (commonSellerId == null) {
                    if (product.sellerId() == null) {
                        log.warn("Product {} has no sellerId!", product.id());
                        throw new BusinessException("Product data integrity error: missing sellerId");
                    }
                    commonSellerId = product.sellerId();

                    // Verify seller account status (unverified sellers cannot sell products)
                    try {
                        var sellerProfile = userServiceClient.getSellerProfile(commonSellerId);
                        if (sellerProfile == null || !Boolean.TRUE.equals(sellerProfile.isVerified())) {
                            throw new BusinessException("Cannot place order: Seller profile is not verified.");
                        }
                    } catch (BusinessException be) {
                        throw be;
                    } catch (Exception e) {
                        log.error("Failed to verify seller {} status: {}", commonSellerId, e.getMessage());
                        throw new BusinessException("Unable to verify seller profile. User service is currently unavailable.");
                    }
                } else {
                    if (!commonSellerId.equals(product.sellerId())) {
                        throw new BusinessException("Multi-vendor orders are not allowed. Please split your order.");
                    }
                }

                // B. Access check (RESTRICTED)
                if ("RESTRICTED".equals(product.accessLevel())) {
                    if (!hasPermission("product:buy:restricted")) {
                        throw new AccessDeniedException(
                                "Purchasing restricted product requires military authorization.");
                    }
                    if (!isVerified) {
                        throw new AccessDeniedException("Account must be verified to purchase restricted items.");
                    }
                }

                // C. REDUCE STOCK
                productIntegrationService.reduceStock(product.id(), itemRequest.quantity());

                // D. Create OrderItem
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

            // 4. ORDER CREATION
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
            log.info("Order created with ID: {}", order.getId());

            // 5. Notification and Payment
            sendNotification(order, productNames, userEmail, purchasedProductIds);

            try {
                userServiceClient.incrementSales(commonSellerId, "");
            } catch (Exception e) {
                log.error("Failed to increment sales for seller {}", commonSellerId, e);
            }

            // If Cash on Delivery (COD) - return success without payment redirect
            if (request.paymentMethod() == PaymentMethod.COD) {
                Boolean autoConfirm = null;
                try {
                    autoConfirm = userServiceClient.getAutoConfirm(commonSellerId);
                } catch (Exception e) {
                    log.error("Failed to fetch auto-confirm config for seller {}", commonSellerId, e);
                }

                if (autoConfirm != null && autoConfirm) {
                    scheduleAutoConfirm(order.getId());
                } else {
                    checkAndScheduleDelivery(order);
                }

                return new PaymentResponseDTO(
                        null,
                        "PENDING_CONFIRMATION",
                        null);
            }

            // If Card - initiate payment
            PaymentRequestDTO paymentRequest = new PaymentRequestDTO(
                    order.getId(),
                    userId,
                    totalOrderPrice);

            return paymentIntegrationService.initPayment(paymentRequest);

        } catch (Exception e) {
            log.error("Error creating order: {}. Rolling back stock...", e.getMessage());
            // Compensation logic
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
                .orElseThrow(() -> new ResourceNotFoundException("Order not found"));
        return mapToOrderResponseDTO(order);
    }

    public Page<OrderResponseDTO> getMyOrders(Pageable pageable) {
        UUID userId = userContext.getUserId();
        // findAllByUserId returns Page<Order>, we map it to Page<OrderResponseDTO>
        return orderRepository.findAllByUserId(userId, pageable)
                .map(this::mapToOrderResponseDTO);
    }

    public Page<OrderResponseDTO> getAllOrders(Pageable pageable) {
        return orderRepository.findAll(pageable)
                .map(this::mapToOrderResponseDTO);
    }

    private void sendNotification(Order order, List<String> productNames, String userEmail, List<UUID> productIds) {
        notificationIntegrationService.publishOrderPlacedEvent(order, productNames, userEmail, productIds);
    }

    // --- Other methods remain unchanged ---

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

        // STRICT REQUIREMENT: Only if delivered
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

    public Page<OrderResponseDTO> getSellerOrders(OrderStatus status, Pageable pageable) {
        UUID sellerId = userContext.getUserId();
        if (status != null) {
            return orderRepository.findAllBySellerIdAndStatus(sellerId, status, pageable)
                    .map(this::mapToOrderResponseDTO);
        }
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
        boolean shouldStartSimulation = false;

        if (order.getStatus() == OrderStatus.PREPARING || order.getStatus() == OrderStatus.CONFIRMED) {
            shouldStartSimulation = true;
        }

        if (shouldStartSimulation) {
            scheduleDelivery(order);
        }
    }

    private void scheduleDelivery(Order order) {
        UUID orderId = order.getId();
        boolean isSelfPickup = order.getDeliveryInfo() != null && order.getDeliveryInfo().type() == DeliveryType.SELF_PICKUP;

        new Thread(() -> {
            try {
                if (isSelfPickup) {
                    log.info("Simulation: Self-pickup order {} is ready for pickup...", orderId);
                    Thread.sleep(3000); // 3 seconds delay to READY_FOR_PICKUP
                    updateOrderStatusFromDelivery(orderId, "READY_TO_SHIP");
                    
                    Thread.sleep(4000); // 4 seconds delay to DELIVERED
                    updateOrderStatusFromDelivery(orderId, "DELIVERED");
                    log.info("Simulation: Self-pickup order {} PICKED UP (DELIVERED)", orderId);
                } else {
                    log.info("Simulation: Shipping order {}", orderId);
                    Thread.sleep(5000); // 5 seconds delay
                    updateOrderStatusFromDelivery(orderId, "DELIVERED");
                    log.info("Simulation: Order {} DELIVERED", orderId);
                }
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }
        }).start();
    }

    private void scheduleAutoConfirm(UUID orderId) {
        new Thread(() -> {
            try {
                log.info("Simulation: Auto-confirming order {} in 3 seconds...", orderId);
                Thread.sleep(3000); // 3 seconds
                
                Order order = orderRepository.findById(orderId).orElse(null);
                if (order != null && order.getStatus() == OrderStatus.PENDING_CONFIRMATION) {
                    order.setStatus(OrderStatus.CONFIRMED);
                    orderRepository.save(order);
                    log.info("Simulation: Order {} AUTO-CONFIRMED", orderId);
                    checkAndScheduleDelivery(order);
                }
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }
        }).start();
    }

    // Updated confirmOrderPayment to check for simulation
    @Transactional
    public void confirmOrderPayment(UUID orderId) {
        log.info("Payment confirmation received for Order: {}", orderId);
        Order order = orderRepository.findById(orderId)
                .orElseThrow(() -> new ResourceNotFoundException("Order not found: " + orderId));

        if (order.getStatus() == OrderStatus.PAID || order.getStatus() == OrderStatus.DELIVERED
                || order.getStatus() == OrderStatus.PENDING_CONFIRMATION) {
            return;
        }

        order.setStatus(OrderStatus.PENDING_CONFIRMATION);
        orderRepository.save(order);

        log.info("Order {} payment confirmed. Status updated to PENDING_CONFIRMATION", orderId);

        Boolean autoConfirm = null;
        try {
            autoConfirm = userServiceClient.getAutoConfirm(order.getSellerId());
        } catch (Exception e) {
            log.error("Failed to fetch auto-confirm config for seller {}", order.getSellerId(), e);
        }
        
        if (autoConfirm != null && autoConfirm) {
            scheduleAutoConfirm(orderId);
        } else {
            checkAndScheduleDelivery(order);
        }
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
                    .provider() == com.milhub.order_service.entity.enums.DeliveryProvider.SELLER;
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

    // --- Cancel & Return Logic ---

    @Transactional
    public void cancelMyOrder(UUID orderId, String reason) {
        UUID userId = userContext.getUserId();
        Order order = orderRepository.findById(orderId)
                .orElseThrow(() -> new ResourceNotFoundException("Order not found: " + orderId));

        if (!order.getUserId().equals(userId)) {
            throw new AccessDeniedException("You can only cancel your own orders");
        }

        // Allow cancellation only for early stages
        if (order.getStatus() != OrderStatus.PENDING_PAYMENT &&
                order.getStatus() != OrderStatus.PENDING_CONFIRMATION &&
                order.getStatus() != OrderStatus.CREATED) {
            throw new BusinessException("Cannot cancel order in status " + order.getStatus() + ". Contact support.");
        }

        log.info("Order {} cancelled by user {}. Reason: {}", orderId, userId, reason);
        self.updateOrderStatus(order, OrderStatus.CANCELLED);

        // Restore stock if it was reserved
        restoreStock(order);
    }

    @Transactional
    public void requestReturn(UUID orderId, String reason) {
        UUID userId = userContext.getUserId();
        Order order = orderRepository.findById(orderId)
                .orElseThrow(() -> new ResourceNotFoundException("Order not found: " + orderId));

        if (!order.getUserId().equals(userId)) {
            throw new AccessDeniedException("You can only request return for your own orders");
        }

        if (order.getStatus() != OrderStatus.DELIVERED) {
            throw new BusinessException(
                    "Return can only be requested for DELIVERED orders. Current status: " + order.getStatus());
        }

        log.info("Return requested for Order {} by User {}. Reason: {}", orderId, userId, reason);
        order.setReturnReason(reason);
        self.updateOrderStatus(order, OrderStatus.RETURN_REQUESTED);
    }

    @Transactional
    public void processReturn(UUID orderId, boolean approved) {
        UUID sellerId = userContext.getUserId();
        Order order = orderRepository.findById(orderId)
                .orElseThrow(() -> new ResourceNotFoundException("Order not found: " + orderId));

        if (!order.getSellerId().equals(sellerId)) {
            throw new AccessDeniedException("You are not the seller of this order");
        }

        if (order.getStatus() != OrderStatus.RETURN_REQUESTED) {
            throw new BusinessException("Order is not in RETURN_REQUESTED state");
        }

        OrderStatus newStatus = approved ? OrderStatus.RETURN_APPROVED : OrderStatus.RETURN_REJECTED;
        log.info("Seller {} processed return for Order {}. Decision: {}", sellerId, orderId, newStatus);

        self.updateOrderStatus(order, newStatus);
    }

    @Transactional
    public void completeReturn(UUID orderId) {
        UUID sellerId = userContext.getUserId();
        Order order = orderRepository.findById(orderId)
                .orElseThrow(() -> new ResourceNotFoundException("Order not found: " + orderId));

        if (!order.getSellerId().equals(sellerId)) {
            throw new AccessDeniedException("You are not the seller of this order");
        }

        if (order.getStatus() != OrderStatus.RETURN_APPROVED) {
            throw new BusinessException("Return must be APPROVED before completing refund.");
        }

        log.info("Seller {} completing return for Order {}. Status -> REFUNDED.", sellerId, orderId);
        self.updateOrderStatus(order, OrderStatus.REFUNDED);

        // Restore stock since item is returned
        restoreStock(order);
    }

    @Transactional
    public void updateOrderStatus(Order order, OrderStatus newStatus) {
        com.milhub.order_service.service.strategy.OrderStatusStrategy strategy = statusStrategies.stream()
                .filter(s -> s.supports(newStatus))
                .findFirst()
                .orElse(defaultStatusStrategy);
        
        strategy.applyStatusChange(order, newStatus);
        orderRepository.save(order);
    }
}