package com.crafthub.order_service.controller;

import com.crafthub.order_service.client.UserServiceClient;
import com.crafthub.order_service.dto.delivery.DeliveryDetailsDTO;
import com.crafthub.order_service.dto.external.ProductResponseDTO;
import com.crafthub.order_service.dto.order.OrderItemRequestDTO;
import com.crafthub.order_service.dto.order.OrderRequestDTO;
import com.crafthub.order_service.dto.payment.PaymentRequestDTO;
import com.crafthub.order_service.dto.payment.PaymentResponseDTO;
import com.crafthub.order_service.entity.Order;
import com.crafthub.order_service.entity.OrderStatus;
import com.crafthub.order_service.entity.enums.DeliveryProvider;
import com.crafthub.order_service.entity.enums.DeliveryType;
import com.crafthub.order_service.entity.enums.PaymentMethod;
import com.crafthub.order_service.repository.OrderRepository;
import com.crafthub.order_service.service.KafkaPublisherService;
import com.crafthub.order_service.service.PaymentIntegrationService;
import com.crafthub.order_service.service.ProductIntegrationService;
import com.crafthub.order_service.service.SqsPublisherService;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;


import org.springframework.test.web.servlet.MockMvc;



import org.junit.jupiter.api.Disabled;

import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@AutoConfigureMockMvc


class OrderControllerIT {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private OrderRepository orderRepository;

    @Autowired
    private ObjectMapper objectMapper;

    @MockBean
    private ProductIntegrationService productIntegrationService;

    @MockBean
    private PaymentIntegrationService paymentIntegrationService;

    @MockBean
    private UserServiceClient userServiceClient;

    @MockBean
    private KafkaPublisherService kafkaPublisherService;

    @MockBean
    private SqsPublisherService sqsPublisherService;

    private UUID userId;
    private UUID sellerId;
    private UUID productId;
    private DeliveryDetailsDTO deliveryDetails;

    @BeforeEach
    void setUp() {
        orderRepository.deleteAll(); // Clean DB before each test

        userId = UUID.randomUUID();
        sellerId = UUID.randomUUID();
        productId = UUID.randomUUID();

        deliveryDetails = DeliveryDetailsDTO.builder()
                .provider(DeliveryProvider.NOVA_POSHTA)
                .type(DeliveryType.BRANCH)
                .cityRef("city1")
                .branchRef("branch1")
                .build();
    }

    @Test
    void createOrder_Integration_Success() throws Exception {
        // Arrange
        OrderRequestDTO request = new OrderRequestDTO(
                List.of(new OrderItemRequestDTO(productId, 2)),
                deliveryDetails,
                PaymentMethod.CARD
        );

        ProductResponseDTO product = new ProductResponseDTO(productId, "Test Item", BigDecimal.valueOf(100), "PUBLIC", 10, sellerId);
        when(productIntegrationService.getProductById(productId)).thenReturn(product);

        PaymentResponseDTO paymentResponse = new PaymentResponseDTO(UUID.randomUUID(), "PENDING", "http://pay.url");
        when(paymentIntegrationService.initPayment(any(PaymentRequestDTO.class))).thenReturn(paymentResponse);

        // Act & Assert
        mockMvc.perform(post("/api/v1/orders")
                        .header("X-User-Id", userId.toString())
                        .header("X-User-Email", "test@example.com")
                        .header("X-User-Role", "BUYER")
                        .header("X-User-Is-Verified", "true")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("PENDING"))
                .andExpect(jsonPath("$.paymentUrl").value("http://pay.url"));

        // Verify Database
        List<Order> orders = orderRepository.findAll();
        assertThat(orders).hasSize(1);
        Order savedOrder = orders.get(0);
        assertThat(savedOrder.getUserId()).isEqualTo(userId);
        assertThat(savedOrder.getSellerId()).isEqualTo(sellerId);
        assertThat(savedOrder.getTotalPrice()).isEqualByComparingTo("200.00");
        assertThat(savedOrder.getStatus()).isEqualTo(OrderStatus.PENDING_PAYMENT);
    }

    @Test
    void createOrder_Integration_InvalidDelivery() throws Exception {
        // Arrange
        OrderRequestDTO request = new OrderRequestDTO(
                List.of(new OrderItemRequestDTO(productId, 2)),
                null, // Invalid
                PaymentMethod.CARD
        );

        // Act & Assert
        mockMvc.perform(post("/api/v1/orders")
                        .header("X-User-Id", userId.toString())
                        .header("X-User-Role", "BUYER")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value("Delivery details are required"));
    }

    @Test
    void getMyOrders_Integration() throws Exception {
        // Arrange
        Order order = Order.builder()
                .userId(userId)
                .sellerId(sellerId)
                .totalPrice(BigDecimal.valueOf(150))
                .status(OrderStatus.DELIVERED)
                .paymentMethod(PaymentMethod.CARD)
                .deliveryInfo(deliveryDetails)
                .build();
        orderRepository.save(order);

        // Act & Assert
        mockMvc.perform(get("/api/v1/orders/my")
                        .header("X-User-Id", userId.toString())
                        .header("X-User-Role", "BUYER"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content.length()").value(1))
                .andExpect(jsonPath("$.content[0].id").value(order.getId().toString()))
                .andExpect(jsonPath("$.content[0].status").value("DELIVERED"));
    }

    @Test
    void getOrderById_Integration() throws Exception {
        // Arrange
        Order order = Order.builder()
                .userId(userId)
                .sellerId(sellerId)
                .totalPrice(BigDecimal.valueOf(150))
                .status(OrderStatus.DELIVERED)
                .paymentMethod(PaymentMethod.CARD)
                .deliveryInfo(deliveryDetails)
                .build();
        orderRepository.save(order);

        // Act & Assert
        mockMvc.perform(get("/api/v1/orders/" + order.getId())
                        .header("X-User-Id", userId.toString())
                        .header("X-User-Role", "BUYER"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(order.getId().toString()))
                .andExpect(jsonPath("$.totalPrice").value(150.0));
    }

    @Test
    void cancelOrder_Integration() throws Exception {
        // Arrange
        Order order = Order.builder()
                .userId(userId)
                .sellerId(sellerId)
                .totalPrice(BigDecimal.valueOf(150))
                .status(OrderStatus.PENDING_PAYMENT)
                .paymentMethod(PaymentMethod.CARD)
                .deliveryInfo(deliveryDetails)
                .build();
        orderRepository.save(order);

        // Act & Assert
        mockMvc.perform(post("/api/v1/orders/" + order.getId() + "/cancel")
                        .header("X-User-Id", userId.toString())
                        .header("X-User-Role", "BUYER")
                        .content("Changed mind"))
                .andExpect(status().isOk());

        Order updatedOrder = orderRepository.findById(order.getId()).orElseThrow();
        assertThat(updatedOrder.getStatus()).isEqualTo(OrderStatus.CANCELLED);
    }

    @Test
    void sellerUpdateStatus_Integration() throws Exception {
        // Arrange
        Order order = Order.builder()
                .userId(userId)
                .sellerId(sellerId)
                .totalPrice(BigDecimal.valueOf(150))
                .status(OrderStatus.PENDING_CONFIRMATION)
                .paymentMethod(PaymentMethod.COD)
                .deliveryInfo(deliveryDetails)
                .build();
        orderRepository.save(order);

        // Act & Assert
        mockMvc.perform(patch("/api/v1/orders/" + order.getId() + "/status")
                        .param("status", "CONFIRMED")
                        .header("X-User-Id", sellerId.toString()) // Seller makes request
                        .header("X-User-Role", "SELLER"))
                .andExpect(status().isOk());

        Order updatedOrder = orderRepository.findById(order.getId()).orElseThrow();
        assertThat(updatedOrder.getStatus()).isEqualTo(OrderStatus.CONFIRMED);
    }

    @Test
    void checkPurchase_Integration() throws Exception {
        // Arrange
        Order order = Order.builder()
                .userId(userId)
                .sellerId(sellerId)
                .totalPrice(BigDecimal.valueOf(150))
                .status(OrderStatus.DELIVERED) // Delivered order makes it a verified purchase
                .paymentMethod(PaymentMethod.CARD)
                .deliveryInfo(deliveryDetails)
                .build();

        com.crafthub.order_service.entity.OrderItem item = com.crafthub.order_service.entity.OrderItem.builder()
                .order(order)
                .productId(productId)
                .quantity(1)
                .pricePerUnit(BigDecimal.valueOf(150))
                .build();
        
        order.setItems(List.of(item));
        orderRepository.save(order);

        // Act & Assert
        mockMvc.perform(get("/api/v1/orders/check-purchase")
                        .param("productId", productId.toString())
                        .header("X-User-Id", userId.toString())
                        .header("X-User-Role", "BUYER"))
                .andExpect(status().isOk())
                .andExpect(content().string("true"));
    }
}
