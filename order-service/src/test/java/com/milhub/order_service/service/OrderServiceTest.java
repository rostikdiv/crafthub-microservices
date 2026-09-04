package com.milhub.order_service.service;

import com.milhub.order_service.client.UserServiceClient;
import com.milhub.order_service.dto.delivery.DeliveryDetailsDTO;
import com.milhub.order_service.dto.event.OrderPlacedEventDTO;
import com.milhub.order_service.dto.external.ProductResponseDTO;
import com.milhub.order_service.dto.order.OrderItemRequestDTO;
import com.milhub.order_service.dto.order.OrderRequestDTO;
import com.milhub.order_service.dto.order.OrderResponseDTO;
import com.milhub.order_service.dto.payment.PaymentRequestDTO;
import com.milhub.order_service.dto.payment.PaymentResponseDTO;
import com.milhub.order_service.entity.Order;
import com.milhub.order_service.entity.OrderItem;
import com.milhub.order_service.entity.OrderStatus;
import com.milhub.order_service.entity.enums.DeliveryProvider;
import com.milhub.order_service.entity.enums.DeliveryType;
import com.milhub.order_service.entity.enums.PaymentMethod;
import com.milhub.order_service.exception.AccessDeniedException;
import com.milhub.order_service.exception.BusinessException;
import com.milhub.order_service.exception.ResourceNotFoundException;
import com.milhub.order_service.repository.OrderRepository;
import com.milhub.order_service.security.UserContextService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Spy;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.test.util.ReflectionTestUtils;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class OrderServiceTest {

    @Mock
    private OrderRepository orderRepository;
    @Mock
    private ProductIntegrationService productIntegrationService;
    @Mock
    private PaymentIntegrationService paymentIntegrationService;
    @Mock
    private UserContextService userContext;
    @Mock
    private UserServiceClient userServiceClient;
    @Mock
    private NotificationIntegrationService notificationIntegrationService;
    @Mock
    private InventoryIntegrationService inventoryIntegrationService;
    @Mock
    private java.util.List<com.milhub.order_service.service.strategy.OrderStatusStrategy> statusStrategies;
    @Spy
    private com.milhub.order_service.service.strategy.DefaultOrderStatusStrategy defaultStatusStrategy;

    @InjectMocks
    private OrderService orderService;

    @Captor
    private ArgumentCaptor<Order> orderCaptor;
    @Captor
    private ArgumentCaptor<OrderPlacedEventDTO> eventCaptor;

    private UUID userId;
    private UUID sellerId;
    private UUID productId;
    private UUID orderId;
    private String userEmail;
    private DeliveryDetailsDTO validDeliveryDetails;

    @BeforeEach
    void setUp() {
        userId = UUID.randomUUID();
        sellerId = UUID.randomUUID();
        productId = UUID.randomUUID();
        orderId = UUID.randomUUID();
        userEmail = "test@example.com";

        validDeliveryDetails = DeliveryDetailsDTO.builder()
                .provider(DeliveryProvider.NOVA_POSHTA)
                .type(DeliveryType.BRANCH)
                .cityRef("city1")
                .branchRef("branch1")
                .build();

        ReflectionTestUtils.setField(orderService, "self", orderService);

        lenient().when(userServiceClient.getSellerProfile(any()))
                .thenReturn(new com.milhub.order_service.dto.seller.SellerPublicProfileDTO(sellerId, "Test Seller", true));
    }

    private void setupSecurityContext(String... authorities) {
        List<SimpleGrantedAuthority> auths = new ArrayList<>();
        for (String auth : authorities) {
            auths.add(new SimpleGrantedAuthority(auth));
        }
        SecurityContextHolder.getContext().setAuthentication(
                new UsernamePasswordAuthenticationToken(userId, null, auths));
    }

    private void mockUserContext(boolean isVerified) {
        lenient().when(userContext.getUserId()).thenReturn(userId);
        lenient().when(userContext.getUserEmail()).thenReturn(userEmail);
        lenient().when(userContext.isVerified()).thenReturn(isVerified);
    }

    // --- CREATE ORDER TESTS ---

    @Test
    void createOrder_Success_CardPayment() {
        mockUserContext(true);
        setupSecurityContext("order:create");

        OrderItemRequestDTO itemRequest = new OrderItemRequestDTO(productId, 2);
        OrderRequestDTO request = new OrderRequestDTO(List.of(itemRequest), validDeliveryDetails, PaymentMethod.CARD);

        ProductResponseDTO product = new ProductResponseDTO(productId, "Test Product", BigDecimal.valueOf(100),
                "PUBLIC", 10, sellerId);
        when(productIntegrationService.getProductById(productId)).thenReturn(product);

        when(orderRepository.save(any(Order.class))).thenAnswer(invocation -> {
            Order o = invocation.getArgument(0);
            o.setId(UUID.randomUUID());
            o.setUserId(userId);
            o.setTotalPrice(BigDecimal.valueOf(200));
            return o;
        });

        PaymentResponseDTO paymentResponse = new PaymentResponseDTO(UUID.randomUUID(), "PENDING", "url");
        when(paymentIntegrationService.initPayment(any(PaymentRequestDTO.class))).thenReturn(paymentResponse);

        PaymentResponseDTO result = orderService.createOrder(request);

        assertThat(result).isNotNull();
        assertThat(result.status()).isEqualTo("PENDING");

        verify(productIntegrationService).reduceStock(productId, 2);
        verify(orderRepository).save(orderCaptor.capture());
        Order capturedOrder = orderCaptor.getValue();
        assertThat(capturedOrder.getStatus()).isEqualTo(OrderStatus.PENDING_PAYMENT);
        assertThat(capturedOrder.getPaymentMethod()).isEqualTo(PaymentMethod.CARD);

        verify(notificationIntegrationService).publishOrderPlacedEvent(any(), any(), any(), any());
    }

    @Test
    void createOrder_Success_COD() {
        mockUserContext(true);
        setupSecurityContext("order:create");

        OrderItemRequestDTO itemRequest = new OrderItemRequestDTO(productId, 1);
        OrderRequestDTO request = new OrderRequestDTO(List.of(itemRequest), validDeliveryDetails, PaymentMethod.COD);

        ProductResponseDTO product = new ProductResponseDTO(productId, "Test Product", BigDecimal.valueOf(150),
                "PUBLIC", 5, sellerId);
        when(productIntegrationService.getProductById(productId)).thenReturn(product);

        Order savedOrder = new Order();
        savedOrder.setId(UUID.randomUUID());
        when(orderRepository.save(any(Order.class))).thenReturn(savedOrder);

        PaymentResponseDTO result = orderService.createOrder(request);

        assertThat(result).isNotNull();
        assertThat(result.status()).isEqualTo("PENDING_CONFIRMATION");

        verify(orderRepository).save(orderCaptor.capture());
        assertThat(orderCaptor.getValue().getStatus()).isEqualTo(OrderStatus.PENDING_CONFIRMATION);
    }

    @Test
    void createOrder_ProductNotFound_ThrowsException() {
        mockUserContext(true);
        OrderRequestDTO request = new OrderRequestDTO(List.of(new OrderItemRequestDTO(productId, 1)),
                validDeliveryDetails, PaymentMethod.CARD);
        when(productIntegrationService.getProductById(productId)).thenReturn(null);

        assertThatThrownBy(() -> orderService.createOrder(request))
                .isInstanceOf(ResourceNotFoundException.class)
                .hasMessageContaining("Product not found");
    }

    @Test
    void createOrder_MultiVendor_ThrowsException() {
        mockUserContext(true);
        UUID productId2 = UUID.randomUUID();
        UUID sellerId2 = UUID.randomUUID();
        OrderRequestDTO request = new OrderRequestDTO(
                List.of(new OrderItemRequestDTO(productId, 1), new OrderItemRequestDTO(productId2, 1)),
                validDeliveryDetails, PaymentMethod.CARD);

        when(productIntegrationService.getProductById(productId)).thenReturn(
                new ProductResponseDTO(productId, "P1", BigDecimal.TEN, "PUBLIC", 10, sellerId));
        when(productIntegrationService.getProductById(productId2)).thenReturn(
                new ProductResponseDTO(productId2, "P2", BigDecimal.TEN, "PUBLIC", 10, sellerId2));

        assertThatThrownBy(() -> orderService.createOrder(request))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("Multi-vendor orders are not allowed");
    }

    @Test
    void createOrder_RestrictedAccessDenied_ThrowsException() {
        mockUserContext(true);
        setupSecurityContext("order:create");
        OrderRequestDTO request = new OrderRequestDTO(List.of(new OrderItemRequestDTO(productId, 1)),
                validDeliveryDetails, PaymentMethod.CARD);

        when(productIntegrationService.getProductById(productId)).thenReturn(
                new ProductResponseDTO(productId, "P1", BigDecimal.TEN, "RESTRICTED", 10, sellerId));

        assertThatThrownBy(() -> orderService.createOrder(request))
                .isInstanceOf(AccessDeniedException.class)
                .hasMessageContaining("Purchasing restricted product requires military authorization");
    }

    @Test
    void createOrder_RestrictedUnverified_ThrowsException() {
        mockUserContext(false);
        setupSecurityContext("order:create", "product:buy:restricted");
        OrderRequestDTO request = new OrderRequestDTO(List.of(new OrderItemRequestDTO(productId, 1)),
                validDeliveryDetails, PaymentMethod.CARD);

        when(productIntegrationService.getProductById(productId)).thenReturn(
                new ProductResponseDTO(productId, "P1", BigDecimal.TEN, "RESTRICTED", 10, sellerId));

        assertThatThrownBy(() -> orderService.createOrder(request))
                .isInstanceOf(AccessDeniedException.class)
                .hasMessageContaining("Account must be verified");
    }

    @Test
    void createOrder_StockRollbackOnFailure() {
        mockUserContext(true);
        setupSecurityContext("order:create");
        OrderRequestDTO request = new OrderRequestDTO(List.of(new OrderItemRequestDTO(productId, 2)),
                validDeliveryDetails, PaymentMethod.CARD);

        when(productIntegrationService.getProductById(productId)).thenReturn(
                new ProductResponseDTO(productId, "P1", BigDecimal.TEN, "PUBLIC", 10, sellerId));

        when(orderRepository.save(any(Order.class))).thenThrow(new RuntimeException("DB Error"));

        assertThatThrownBy(() -> orderService.createOrder(request))
                .isInstanceOf(RuntimeException.class)
                .hasMessageContaining("DB Error");

        verify(productIntegrationService).restoreStock(productId, 2);
    }

    @Test
    void createOrder_InvalidDeliveryDetails() {
        mockUserContext(true);
        OrderRequestDTO request = new OrderRequestDTO(List.of(new OrderItemRequestDTO(productId, 1)), null,
                PaymentMethod.CARD);

        assertThatThrownBy(() -> orderService.createOrder(request))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("Delivery details are required");
    }

    // --- PAYMENT CONFIRMATION TESTS ---

    @Test
    void confirmOrderPayment_Success() {
        UUID orderId = UUID.randomUUID();
        Order order = new Order();
        order.setId(orderId);
        order.setStatus(OrderStatus.PENDING_PAYMENT);

        when(orderRepository.findById(orderId)).thenReturn(Optional.of(order));

        orderService.confirmOrderPayment(orderId);

        verify(orderRepository).save(orderCaptor.capture());
        assertThat(orderCaptor.getValue().getStatus()).isEqualTo(OrderStatus.PENDING_CONFIRMATION);
    }

    @Test
    void confirmOrderPayment_AlreadyPaid_NoChange() {
        UUID orderId = UUID.randomUUID();
        Order order = new Order();
        order.setId(orderId);
        order.setStatus(OrderStatus.PAID);

        when(orderRepository.findById(orderId)).thenReturn(Optional.of(order));

        orderService.confirmOrderPayment(orderId);

        verify(orderRepository, never()).save(any());
    }

    // --- CANCELLATION TESTS ---

    @Test
    void cancelMyOrder_Success() {
        mockUserContext(true);
        UUID orderId = UUID.randomUUID();
        Order order = new Order();
        order.setId(orderId);
        order.setUserId(userId);
        order.setStatus(OrderStatus.PENDING_PAYMENT);
        order.setItems(List.of(new OrderItem(1L, productId, "P1", 2, BigDecimal.TEN, order)));

        when(orderRepository.findById(orderId)).thenReturn(Optional.of(order));

        orderService.cancelMyOrder(orderId, "Changed mind");

        verify(orderRepository).save(orderCaptor.capture());
        assertThat(orderCaptor.getValue().getStatus()).isEqualTo(OrderStatus.CANCELLED);
        verify(productIntegrationService).restoreStock(productId, 2);
    }

    @Test
    void cancelMyOrder_WrongUser_ThrowsException() {
        mockUserContext(true);
        UUID orderId = UUID.randomUUID();
        Order order = new Order();
        order.setId(orderId);
        order.setUserId(UUID.randomUUID());

        when(orderRepository.findById(orderId)).thenReturn(Optional.of(order));

        assertThatThrownBy(() -> orderService.cancelMyOrder(orderId, "Reason"))
                .isInstanceOf(AccessDeniedException.class)
                .hasMessageContaining("You can only cancel your own orders");
    }

    @Test
    void cancelMyOrder_InvalidStatus_ThrowsException() {
        mockUserContext(true);
        UUID orderId = UUID.randomUUID();
        Order order = new Order();
        order.setId(orderId);
        order.setUserId(userId);
        order.setStatus(OrderStatus.SHIPPED);

        when(orderRepository.findById(orderId)).thenReturn(Optional.of(order));

        assertThatThrownBy(() -> orderService.cancelMyOrder(orderId, "Reason"))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("Cannot cancel order in status");
    }

    // --- SELLER UPDATE TESTS ---

    @Test
    void updateOrderStatusBySeller_Success() {
        userId = sellerId;
        mockUserContext(true);

        UUID orderId = UUID.randomUUID();
        Order order = new Order();
        order.setId(orderId);
        order.setSellerId(sellerId);
        order.setStatus(OrderStatus.PENDING_CONFIRMATION);
        order.setDeliveryInfo(validDeliveryDetails);

        when(orderRepository.findById(orderId)).thenReturn(Optional.of(order));
        when(orderRepository.save(any(Order.class))).thenReturn(order);

        OrderResponseDTO response = orderService.updateOrderStatusBySeller(orderId, OrderStatus.CONFIRMED);

        assertThat(response).isNotNull();
        verify(orderRepository).save(orderCaptor.capture());
        assertThat(orderCaptor.getValue().getStatus()).isEqualTo(OrderStatus.CONFIRMED);
    }

    @Test
    void updateOrderStatusBySeller_NotOwner_ThrowsException() {
        mockUserContext(true);

        UUID orderId = UUID.randomUUID();
        Order order = new Order();
        order.setId(orderId);
        order.setSellerId(UUID.randomUUID());

        when(orderRepository.findById(orderId)).thenReturn(Optional.of(order));

        assertThatThrownBy(() -> orderService.updateOrderStatusBySeller(orderId, OrderStatus.CONFIRMED))
                .isInstanceOf(AccessDeniedException.class)
                .hasMessageContaining("You are not the seller of this order");
    }

    // --- RETURN PROCESS TESTS ---

    @Test
    void requestReturn_Success() {
        mockUserContext(true);
        UUID orderId = UUID.randomUUID();
        Order order = new Order();
        order.setId(orderId);
        order.setUserId(userId);
        order.setStatus(OrderStatus.DELIVERED);

        when(orderRepository.findById(orderId)).thenReturn(Optional.of(order));

        orderService.requestReturn(orderId, "Defective");

        verify(orderRepository).save(orderCaptor.capture());
        assertThat(orderCaptor.getValue().getStatus()).isEqualTo(OrderStatus.RETURN_REQUESTED);
    }

    @Test
    void processReturn_Approved() {
        userId = sellerId;
        mockUserContext(true);

        UUID orderId = UUID.randomUUID();
        Order order = new Order();
        order.setId(orderId);
        order.setSellerId(sellerId);
        order.setStatus(OrderStatus.RETURN_REQUESTED);

        when(orderRepository.findById(orderId)).thenReturn(Optional.of(order));

        orderService.processReturn(orderId, true);

        verify(orderRepository).save(orderCaptor.capture());
        assertThat(orderCaptor.getValue().getStatus()).isEqualTo(OrderStatus.RETURN_APPROVED);
    }

    @Test
    void completeReturn_Success() {
        userId = sellerId;
        mockUserContext(true);

        UUID orderId = UUID.randomUUID();
        Order order = new Order();
        order.setId(orderId);
        order.setSellerId(sellerId);
        order.setStatus(OrderStatus.RETURN_APPROVED);
        order.setItems(List.of(new OrderItem(1L, productId, "P1", 1, BigDecimal.TEN, order)));

        when(orderRepository.findById(orderId)).thenReturn(Optional.of(order));

        orderService.completeReturn(orderId);

        verify(orderRepository).save(orderCaptor.capture());
        assertThat(orderCaptor.getValue().getStatus()).isEqualTo(OrderStatus.REFUNDED);
        verify(productIntegrationService).restoreStock(productId, 1);
    }

    // --- DELIVERY STATUS UPDATE TESTS ---

    @Test
    void updateOrderStatusFromDelivery_Shipped() {
        UUID orderId = UUID.randomUUID();
        Order order = new Order();
        order.setId(orderId);
        order.setStatus(OrderStatus.PREPARING);

        when(orderRepository.findById(orderId)).thenReturn(Optional.of(order));

        orderService.updateOrderStatusFromDelivery(orderId, "SHIPPED");

        verify(orderRepository).save(orderCaptor.capture());
        assertThat(orderCaptor.getValue().getStatus()).isEqualTo(OrderStatus.SHIPPED);
    }

    @Test
    void updateOrderStatusFromDelivery_ReadyForPickup_ForSelfPickup() {
        UUID orderId = UUID.randomUUID();
        Order order = new Order();
        order.setId(orderId);
        order.setStatus(OrderStatus.PREPARING);
        order.setDeliveryInfo(DeliveryDetailsDTO.builder().type(DeliveryType.SELF_PICKUP).build());

        when(orderRepository.findById(orderId)).thenReturn(Optional.of(order));

        orderService.updateOrderStatusFromDelivery(orderId, "READY_TO_SHIP");

        verify(orderRepository).save(orderCaptor.capture());
        assertThat(orderCaptor.getValue().getStatus()).isEqualTo(OrderStatus.READY_FOR_PICKUP);
    }

    // --- QUERY TESTS ---

    @Test
    void getOrderById_Success() {
        UUID orderId = UUID.randomUUID();
        Order order = new Order();
        order.setId(orderId);
        order.setUserId(userId);
        order.setTotalPrice(BigDecimal.TEN);
        order.setStatus(OrderStatus.CREATED);
        order.setItems(List.of());
        order.setDeliveryInfo(validDeliveryDetails);
        order.setCreatedAt(LocalDateTime.now());

        when(orderRepository.findById(orderId)).thenReturn(Optional.of(order));

        OrderResponseDTO response = orderService.getOrderById(orderId);

        assertThat(response).isNotNull();
        assertThat(response.id()).isEqualTo(orderId);
    }

    @Test
    void getMyOrders_ReturnsPage() {
        mockUserContext(true);
        Pageable pageable = PageRequest.of(0, 10);

        Order order = new Order();
        order.setId(UUID.randomUUID());
        order.setItems(List.of());
        Page<Order> page = new PageImpl<>(List.of(order));

        when(orderRepository.findAllByUserId(userId, pageable)).thenReturn(page);

        Page<OrderResponseDTO> result = orderService.getMyOrders(pageable);

        assertThat(result).isNotNull();
        assertThat(result.getContent()).hasSize(1);
    }

    @Test
    void createOrder_MissingSellerId_ThrowsException() {
        mockUserContext(true);
        OrderRequestDTO request = new OrderRequestDTO(
                List.of(new OrderItemRequestDTO(productId, 1)),
                validDeliveryDetails, PaymentMethod.CARD);

        when(productIntegrationService.getProductById(productId)).thenReturn(
                new ProductResponseDTO(productId, "P1", BigDecimal.TEN, "PUBLIC", 10, null));

        assertThatThrownBy(() -> orderService.createOrder(request))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("Product data integrity error: missing sellerId");
    }

    @Test
    void cancelOrder_OrderNotFound_ThrowsException() {
        mockUserContext(false);
        when(orderRepository.findById(any())).thenReturn(Optional.empty());

        assertThatThrownBy(() -> orderService.cancelMyOrder(UUID.randomUUID(), "Reason"))
                .isInstanceOf(ResourceNotFoundException.class)
                .hasMessageContaining("Order not found");
    }

    @Test
    void cancelOrder_AlreadyCancelled_ThrowsException() {
        mockUserContext(false);
        Order order = new Order();
        order.setUserId(userId);
        order.setStatus(OrderStatus.CANCELLED);
        when(orderRepository.findById(orderId)).thenReturn(Optional.of(order));

        assertThatThrownBy(() -> orderService.cancelMyOrder(orderId, "Reason"))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("Cannot cancel order in status CANCELLED. Contact support.");
    }

    @Test
    void cancelOrder_AlreadyShipped_ThrowsException() {
        mockUserContext(false);
        Order order = new Order();
        order.setUserId(userId);
        order.setStatus(OrderStatus.SHIPPED);
        when(orderRepository.findById(orderId)).thenReturn(Optional.of(order));

        assertThatThrownBy(() -> orderService.cancelMyOrder(orderId, "Reason"))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("Cannot cancel order in status SHIPPED. Contact support.");
    }

    @Test
    void handlePaymentConfirmation_OrderNotFound_ThrowsException() {
        when(orderRepository.findById(any())).thenReturn(Optional.empty());

        assertThatThrownBy(() -> orderService.confirmOrderPayment(UUID.randomUUID()))
                .isInstanceOf(ResourceNotFoundException.class)
                .hasMessageContaining("Order not found");
    }

    @Test
    void handlePaymentConfirmation_OrderAlreadyPaid_ThrowsException() {
        Order order = new Order();
        order.setStatus(OrderStatus.PENDING_CONFIRMATION);
        when(orderRepository.findById(orderId)).thenReturn(Optional.of(order));

        orderService.confirmOrderPayment(orderId);
        verify(orderRepository, never()).save(any());
    }

    @Test
    void sellerUpdateStatus_NotSeller_ThrowsException() {
        lenient().when(userContext.getUserId()).thenReturn(UUID.randomUUID());

        Order order = new Order();
        order.setSellerId(sellerId);
        when(orderRepository.findById(orderId)).thenReturn(Optional.of(order));

        assertThatThrownBy(() -> orderService.updateOrderStatusBySeller(orderId, OrderStatus.CONFIRMED))
                .isInstanceOf(AccessDeniedException.class)
                .hasMessageContaining("You are not the seller of this order");
    }

    @Test
    void handleDeliveryUpdate_OrderNotFound_ThrowsException() {
        when(orderRepository.findById(any())).thenReturn(Optional.empty());

        assertThatThrownBy(() -> orderService.updateOrderStatusFromDelivery(UUID.randomUUID(), "SHIPPED"))
                .isInstanceOf(ResourceNotFoundException.class)
                .hasMessageContaining("Order not found");
    }

    @Test
    void getOrderById_OrderNotFound_ThrowsException() {
        mockUserContext(false);
        when(orderRepository.findById(any())).thenReturn(Optional.empty());

        assertThatThrownBy(() -> orderService.getOrderById(UUID.randomUUID()))
                .isInstanceOf(ResourceNotFoundException.class)
                .hasMessageContaining("Order not found");
    }

    @Test
    void getAllOrders_ReturnsList() {
        Order order = new Order();
        order.setId(orderId);
        order.setItems(List.of());
        when(orderRepository.findAll(any(org.springframework.data.domain.Pageable.class))).thenReturn(new org.springframework.data.domain.PageImpl<>(List.of(order)));

        var result = orderService.getAllOrders(org.springframework.data.domain.Pageable.unpaged());

        assertThat(result).hasSize(1);
    }

    @Test
    void updateOrderStatusBySeller_OrderNotFound_ThrowsException() {
        mockUserContext(true);
        when(orderRepository.findById(any())).thenReturn(Optional.empty());

        assertThatThrownBy(() -> orderService.updateOrderStatusBySeller(UUID.randomUUID(), OrderStatus.CONFIRMED))
                .isInstanceOf(ResourceNotFoundException.class)
                .hasMessageContaining("Order not found");
    }

    @Test
    void completeReturn_NotApproved_ThrowsException() {
        userId = sellerId;
        mockUserContext(true);
        Order order = new Order();
        order.setSellerId(sellerId);
        order.setStatus(OrderStatus.RETURN_REQUESTED);
        when(orderRepository.findById(orderId)).thenReturn(Optional.of(order));

        assertThatThrownBy(() -> orderService.completeReturn(orderId))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("Return must be APPROVED before completing refund.");
    }

    @Test
    void createOrder_SellerProfileNotVerified_ThrowsBusinessException() {
        mockUserContext(true);
        when(userServiceClient.getSellerProfile(sellerId))
                .thenReturn(new com.milhub.order_service.dto.seller.SellerPublicProfileDTO(sellerId, "Unverified Seller", false));

        OrderItemRequestDTO itemRequest = new OrderItemRequestDTO(productId, 1);
        OrderRequestDTO request = new OrderRequestDTO(List.of(itemRequest), validDeliveryDetails, PaymentMethod.CARD);
        ProductResponseDTO product = new ProductResponseDTO(productId, "Item", BigDecimal.TEN, "PUBLIC", 5, sellerId);
        when(productIntegrationService.getProductById(productId)).thenReturn(product);

        assertThatThrownBy(() -> orderService.createOrder(request))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("Seller profile is not verified");
    }

    @Test
    void createOrder_UserServiceUnavailable_ThrowsBusinessException() {
        mockUserContext(true);
        when(userServiceClient.getSellerProfile(sellerId))
                .thenThrow(new RuntimeException("Service down"));

        OrderItemRequestDTO itemRequest = new OrderItemRequestDTO(productId, 1);
        OrderRequestDTO request = new OrderRequestDTO(List.of(itemRequest), validDeliveryDetails, PaymentMethod.CARD);
        ProductResponseDTO product = new ProductResponseDTO(productId, "Item", BigDecimal.TEN, "PUBLIC", 5, sellerId);
        when(productIntegrationService.getProductById(productId)).thenReturn(product);

        assertThatThrownBy(() -> orderService.createOrder(request))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("User service is currently unavailable");
    }

    @Test
    void createOrder_DeliveryDetailsWithoutEmail_SetsUserEmail() {
        mockUserContext(true);
        setupSecurityContext("order:create");

        DeliveryDetailsDTO detailsWithoutEmail = DeliveryDetailsDTO.builder()
                .provider(DeliveryProvider.NOVA_POSHTA)
                .type(DeliveryType.BRANCH)
                .cityRef("City")
                .branchRef("1")
                .recipientEmail(null)
                .build();

        OrderItemRequestDTO itemRequest = new OrderItemRequestDTO(productId, 1);
        OrderRequestDTO request = new OrderRequestDTO(List.of(itemRequest), detailsWithoutEmail, PaymentMethod.COD);
        ProductResponseDTO product = new ProductResponseDTO(productId, "Item", BigDecimal.TEN, "PUBLIC", 5, sellerId);
        when(productIntegrationService.getProductById(productId)).thenReturn(product);
        when(userServiceClient.getAutoConfirm(sellerId)).thenReturn(false);

        PaymentResponseDTO response = orderService.createOrder(request);
        assertThat(response.status()).isEqualTo("PENDING_CONFIRMATION");
    }

    @Test
    void createOrder_UserServiceExceptionsIgnoredGracefully() {
        mockUserContext(true);
        setupSecurityContext("order:create");

        OrderItemRequestDTO itemRequest = new OrderItemRequestDTO(productId, 1);
        OrderRequestDTO request = new OrderRequestDTO(List.of(itemRequest), validDeliveryDetails, PaymentMethod.COD);
        ProductResponseDTO product = new ProductResponseDTO(productId, "Item", BigDecimal.TEN, "PUBLIC", 5, sellerId);
        when(productIntegrationService.getProductById(productId)).thenReturn(product);

        doThrow(new RuntimeException("increment sales failed")).when(userServiceClient).incrementSales(any(), any());
        when(userServiceClient.getAutoConfirm(any())).thenThrow(new RuntimeException("auto confirm error"));

        PaymentResponseDTO response = orderService.createOrder(request);
        assertThat(response.status()).isEqualTo("PENDING_CONFIRMATION");
    }

    @Test
    void createOrder_CompensatingTransactionFailureLogged() {
        mockUserContext(true);

        OrderItemRequestDTO itemRequest = new OrderItemRequestDTO(productId, 1);
        OrderRequestDTO request = new OrderRequestDTO(List.of(itemRequest), validDeliveryDetails, PaymentMethod.CARD);
        ProductResponseDTO product = new ProductResponseDTO(productId, "Item", BigDecimal.TEN, "PUBLIC", 5, sellerId);
        when(productIntegrationService.getProductById(productId)).thenReturn(product);

        doThrow(new RuntimeException("DB save failed")).when(orderRepository).save(any());
        doThrow(new RuntimeException("Restore stock failed")).when(productIntegrationService).restoreStock(any(), any());

        assertThatThrownBy(() -> orderService.createOrder(request))
                .isInstanceOf(RuntimeException.class);
    }

    @Test
    void mapToOrderResponseDTO_ItemNameNull_DefaultsToUnknown() {
        UUID orderId = UUID.randomUUID();
        Order order = Order.builder()
                .id(orderId)
                .userId(userId)
                .status(OrderStatus.CREATED)
                .totalPrice(BigDecimal.TEN)
                .createdAt(LocalDateTime.now())
                .items(List.of(OrderItem.builder().productId(productId).name(null).quantity(1).pricePerUnit(BigDecimal.TEN).build()))
                .build();
        when(orderRepository.findById(orderId)).thenReturn(Optional.of(order));

        OrderResponseDTO result = orderService.getOrderById(orderId);
        assertThat(result.items().get(0).name()).isEqualTo("Unknown Product");
    }

    @Test
    void updateOrderStatusFromDelivery_AllDeliveryStatuses() {
        Order order = new Order();
        order.setId(orderId);
        order.setStatus(OrderStatus.CONFIRMED);
        order.setDeliveryInfo(validDeliveryDetails);
        when(orderRepository.findById(orderId)).thenReturn(Optional.of(order));

        // PREPARING
        orderService.updateOrderStatusFromDelivery(orderId, "PREPARING");
        assertThat(order.getStatus()).isEqualTo(OrderStatus.PREPARING);

        // SHIPPED
        orderService.updateOrderStatusFromDelivery(orderId, "SHIPPED");
        assertThat(order.getStatus()).isEqualTo(OrderStatus.SHIPPED);

        // DELIVERED
        orderService.updateOrderStatusFromDelivery(orderId, "DELIVERED");
        assertThat(order.getStatus()).isEqualTo(OrderStatus.DELIVERED);

        // CANCELLED
        orderService.updateOrderStatusFromDelivery(orderId, "CANCELLED");
        assertThat(order.getStatus()).isEqualTo(OrderStatus.CANCELLED);

        // READY_TO_SHIP with non-SELF_PICKUP
        orderService.updateOrderStatusFromDelivery(orderId, "READY_TO_SHIP");
        assertThat(order.getStatus()).isEqualTo(OrderStatus.PREPARING);

        // READY_TO_SHIP with SELF_PICKUP
        DeliveryDetailsDTO pickupDelivery = DeliveryDetailsDTO.builder()
                .provider(DeliveryProvider.SELLER)
                .type(DeliveryType.SELF_PICKUP)
                .pickupAddress("Main Base")
                .build();
        order.setDeliveryInfo(pickupDelivery);
        orderService.updateOrderStatusFromDelivery(orderId, "READY_TO_SHIP");
        assertThat(order.getStatus()).isEqualTo(OrderStatus.READY_FOR_PICKUP);
    }

    @Test
    void hasUserPurchasedProduct_ReturnsRepositoryResult() {
        mockUserContext(true);
        when(orderRepository.existsByUserIdAndItemsProductIdAndStatusIn(eq(userId), eq(productId), anyList()))
                .thenReturn(true);

        boolean result = orderService.hasUserPurchasedProduct(productId);
        assertThat(result).isTrue();
    }

    @Test
    void hasUserBoughtFromSeller_ReturnsRepositoryResult() {
        when(orderRepository.existsByUserIdAndSellerIdAndStatusIn(eq(userId), eq(sellerId), anyList()))
                .thenReturn(true);

        boolean result = orderService.hasUserBoughtFromSeller(userId, sellerId);
        assertThat(result).isTrue();
    }

    @Test
    void getSellerOrders_StatusNullAndNotNull() {
        lenient().when(userContext.getUserId()).thenReturn(sellerId);
        Pageable pageable = PageRequest.of(0, 10);
        Order order = new Order();
        order.setId(orderId);
        order.setItems(List.of());
        Page<Order> page = new PageImpl<>(List.of(order));

        when(orderRepository.findAllBySellerIdAndStatus(sellerId, OrderStatus.DELIVERED, pageable)).thenReturn(page);
        when(orderRepository.findAllBySellerId(sellerId, pageable)).thenReturn(page);

        var resWithStatus = orderService.getSellerOrders(OrderStatus.DELIVERED, pageable);
        assertThat(resWithStatus).hasSize(1);

        var resWithoutStatus = orderService.getSellerOrders(null, pageable);
        assertThat(resWithoutStatus).hasSize(1);
    }

    @Test
    void updateOrderStatusBySeller_CancelOrder_RestoresStock() {
        lenient().when(userContext.getUserId()).thenReturn(sellerId);
        Order order = new Order();
        order.setId(orderId);
        order.setSellerId(sellerId);
        order.setStatus(OrderStatus.CONFIRMED);
        OrderItem item = OrderItem.builder().productId(productId).quantity(2).pricePerUnit(BigDecimal.TEN).build();
        order.setItems(List.of(item));
        when(orderRepository.findById(orderId)).thenReturn(Optional.of(order));
        when(orderRepository.save(any())).thenReturn(order);

        OrderResponseDTO result = orderService.updateOrderStatusBySeller(orderId, OrderStatus.CANCELLED);
        assertThat(result.status()).isEqualTo(OrderStatus.CANCELLED);
        verify(productIntegrationService).restoreStock(productId, 2);
    }

    @Test
    void validateDeliveryDetails_AllValidationBranches() {
        mockUserContext(true);
        setupSecurityContext("order:create");
        OrderItemRequestDTO itemRequest = new OrderItemRequestDTO(productId, 1);

        // 1. Details null
        OrderRequestDTO req1 = new OrderRequestDTO(List.of(itemRequest), null, PaymentMethod.CARD);
        assertThatThrownBy(() -> orderService.createOrder(req1))
                .isInstanceOf(BusinessException.class).hasMessageContaining("Delivery details are required");

        // 2. Provider null
        DeliveryDetailsDTO noProvider = DeliveryDetailsDTO.builder().provider(null).type(DeliveryType.BRANCH).build();
        OrderRequestDTO req2 = new OrderRequestDTO(List.of(itemRequest), noProvider, PaymentMethod.CARD);
        assertThatThrownBy(() -> orderService.createOrder(req2))
                .isInstanceOf(BusinessException.class).hasMessageContaining("Delivery provider and type are required");

        // 3. Branch missing city/branch
        DeliveryDetailsDTO noBranch = DeliveryDetailsDTO.builder().provider(DeliveryProvider.NOVA_POSHTA).type(DeliveryType.BRANCH).cityRef(null).build();
        OrderRequestDTO req3 = new OrderRequestDTO(List.of(itemRequest), noBranch, PaymentMethod.CARD);
        assertThatThrownBy(() -> orderService.createOrder(req3))
                .isInstanceOf(BusinessException.class).hasMessageContaining("City and Branch are required");

        // 4. Courier non-seller missing city
        DeliveryDetailsDTO courierNoCity = DeliveryDetailsDTO.builder().provider(DeliveryProvider.NOVA_POSHTA).type(DeliveryType.COURIER).cityRef(null).street("Street").building("1").build();
        OrderRequestDTO req4 = new OrderRequestDTO(List.of(itemRequest), courierNoCity, PaymentMethod.CARD);
        assertThatThrownBy(() -> orderService.createOrder(req4))
                .isInstanceOf(BusinessException.class).hasMessageContaining("Address details are required for COURIER");

        // 5. Courier missing street/building
        DeliveryDetailsDTO courierNoStreet = DeliveryDetailsDTO.builder().provider(DeliveryProvider.SELLER).type(DeliveryType.COURIER).street(null).building(null).build();
        OrderRequestDTO req5 = new OrderRequestDTO(List.of(itemRequest), courierNoStreet, PaymentMethod.CARD);
        assertThatThrownBy(() -> orderService.createOrder(req5))
                .isInstanceOf(BusinessException.class).hasMessageContaining("Address details are required for COURIER");

        // 6. Self pickup missing address
        DeliveryDetailsDTO pickupNoAddr = DeliveryDetailsDTO.builder().provider(DeliveryProvider.SELLER).type(DeliveryType.SELF_PICKUP).pickupAddress(null).build();
        OrderRequestDTO req6 = new OrderRequestDTO(List.of(itemRequest), pickupNoAddr, PaymentMethod.CARD);
        assertThatThrownBy(() -> orderService.createOrder(req6))
                .isInstanceOf(BusinessException.class).hasMessageContaining("Pickup address is required");
    }

    @Test
    void cancelMyOrder_WrongUser_ThrowsAccessDenied() {
        mockUserContext(true);
        Order order = new Order();
        order.setUserId(UUID.randomUUID()); // Different user
        when(orderRepository.findById(orderId)).thenReturn(Optional.of(order));

        assertThatThrownBy(() -> orderService.cancelMyOrder(orderId, "reason"))
                .isInstanceOf(AccessDeniedException.class)
                .hasMessageContaining("You can only cancel your own orders");
    }

    @Test
    void requestReturn_WrongUser_ThrowsAccessDenied() {
        mockUserContext(true);
        Order order = new Order();
        order.setUserId(UUID.randomUUID());
        when(orderRepository.findById(orderId)).thenReturn(Optional.of(order));

        assertThatThrownBy(() -> orderService.requestReturn(orderId, "Defect"))
                .isInstanceOf(AccessDeniedException.class)
                .hasMessageContaining("You can only request return for your own orders");
    }

    @Test
    void requestReturn_NotDelivered_ThrowsBusinessException() {
        mockUserContext(true);
        Order order = new Order();
        order.setUserId(userId);
        order.setStatus(OrderStatus.SHIPPED);
        when(orderRepository.findById(orderId)).thenReturn(Optional.of(order));

        assertThatThrownBy(() -> orderService.requestReturn(orderId, "Defect"))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("Return can only be requested for DELIVERED orders");
    }

    @Test
    void processReturn_WrongSeller_ThrowsAccessDenied() {
        lenient().when(userContext.getUserId()).thenReturn(UUID.randomUUID());
        Order order = new Order();
        order.setSellerId(sellerId);
        when(orderRepository.findById(orderId)).thenReturn(Optional.of(order));

        assertThatThrownBy(() -> orderService.processReturn(orderId, true))
                .isInstanceOf(AccessDeniedException.class)
                .hasMessageContaining("You are not the seller of this order");
    }

    @Test
    void processReturn_InvalidStatus_ThrowsBusinessException() {
        lenient().when(userContext.getUserId()).thenReturn(sellerId);
        Order order = new Order();
        order.setSellerId(sellerId);
        order.setStatus(OrderStatus.DELIVERED);
        when(orderRepository.findById(orderId)).thenReturn(Optional.of(order));

        assertThatThrownBy(() -> orderService.processReturn(orderId, true))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("Order is not in RETURN_REQUESTED state");
    }

    @Test
    void completeReturn_WrongSeller_ThrowsAccessDenied() {
        lenient().when(userContext.getUserId()).thenReturn(UUID.randomUUID());
        Order order = new Order();
        order.setSellerId(sellerId);
        when(orderRepository.findById(orderId)).thenReturn(Optional.of(order));

        assertThatThrownBy(() -> orderService.completeReturn(orderId))
                .isInstanceOf(AccessDeniedException.class)
                .hasMessageContaining("You are not the seller of this order");
    }

    @Test
    void confirmOrderPayment_AlreadyDeliveredOrPaid_ReturnsImmediately() {
        Order orderDelivered = new Order();
        orderDelivered.setStatus(OrderStatus.DELIVERED);
        when(orderRepository.findById(orderId)).thenReturn(Optional.of(orderDelivered));

        orderService.confirmOrderPayment(orderId);
        verify(orderRepository, never()).save(any());

        Order orderPaid = new Order();
        orderPaid.setStatus(OrderStatus.PAID);
        when(orderRepository.findById(orderId)).thenReturn(Optional.of(orderPaid));

        orderService.confirmOrderPayment(orderId);
        verify(orderRepository, never()).save(any());
    }

    @Test
    void confirmOrderPayment_AutoConfirmConfigured_UpdatesStatus() {
        Order order = new Order();
        order.setId(orderId);
        order.setSellerId(sellerId);
        order.setStatus(OrderStatus.PENDING_PAYMENT);
        when(orderRepository.findById(orderId)).thenReturn(Optional.of(order));
        when(userServiceClient.getAutoConfirm(sellerId)).thenReturn(true);

        orderService.confirmOrderPayment(orderId);

        assertThat(order.getStatus()).isEqualTo(OrderStatus.PENDING_CONFIRMATION);
        verify(orderRepository).save(order);
    }
}
