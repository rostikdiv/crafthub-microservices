package com.milhub.order_service.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.milhub.order_service.dto.delivery.DeliveryDetailsDTO;
import com.milhub.order_service.dto.order.OrderItemRequestDTO;
import com.milhub.order_service.dto.order.OrderRequestDTO;
import com.milhub.order_service.dto.order.OrderResponseDTO;
import com.milhub.order_service.dto.payment.PaymentResponseDTO;
import com.milhub.order_service.entity.OrderStatus;
import com.milhub.order_service.entity.enums.DeliveryProvider;
import com.milhub.order_service.entity.enums.DeliveryType;
import com.milhub.order_service.entity.enums.PaymentMethod;
import com.milhub.order_service.service.OrderService;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.http.MediaType;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.web.servlet.MockMvc;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.Collections;
import java.util.List;
import java.util.UUID;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(OrderController.class)
@AutoConfigureMockMvc(addFilters = false)
class OrderControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockBean
    private OrderService orderService;

    @Test
    @WithMockUser(authorities = "order:create")
    @DisplayName("cancelOrder calls orderService and returns 200 OK")
    void testCancelOrder() throws Exception {
        UUID orderId = UUID.randomUUID();
        doNothing().when(orderService).cancelMyOrder(eq(orderId), any());

        mockMvc.perform(post("/api/v1/orders/{id}/cancel", orderId)
                        .content("Changed mind")
                        .contentType(MediaType.TEXT_PLAIN))
                .andExpect(status().isOk());

        verify(orderService).cancelMyOrder(eq(orderId), eq("Changed mind"));
    }

    @Test
    @WithMockUser(authorities = "order:create")
    @DisplayName("createOrder calls orderService and returns PaymentResponseDTO")
    void testCreateOrder() throws Exception {
        UUID orderId = UUID.randomUUID();
        DeliveryDetailsDTO deliveryDetails = DeliveryDetailsDTO.builder()
                .provider(DeliveryProvider.NOVA_POSHTA)
                .type(DeliveryType.BRANCH)
                .cityRef("Kyiv")
                .branchRef("1")
                .build();
        OrderRequestDTO request = new OrderRequestDTO(
                List.of(new OrderItemRequestDTO(UUID.randomUUID(), 2)),
                deliveryDetails,
                PaymentMethod.CARD
        );

        PaymentResponseDTO paymentResponse = new PaymentResponseDTO(UUID.randomUUID(), "PENDING", "https://pay.url");
        when(orderService.createOrder(any(OrderRequestDTO.class))).thenReturn(paymentResponse);

        mockMvc.perform(post("/api/v1/orders")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("PENDING"))
                .andExpect(jsonPath("$.paymentUrl").value("https://pay.url"));

        verify(orderService).createOrder(any(OrderRequestDTO.class));
    }

    @Test
    @WithMockUser(authorities = "order:create")
    @DisplayName("requestReturn calls orderService and returns 200 OK")
    void testRequestReturn() throws Exception {
        UUID orderId = UUID.randomUUID();
        OrderController.SimpleReturnRequest returnRequest = new OrderController.SimpleReturnRequest("Wrong size");
        doNothing().when(orderService).requestReturn(eq(orderId), eq("Wrong size"));

        mockMvc.perform(post("/api/v1/orders/{id}/return", orderId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(returnRequest)))
                .andExpect(status().isOk());

        verify(orderService).requestReturn(eq(orderId), eq("Wrong size"));
    }

    @Test
    @WithMockUser(roles = "SELLER")
    @DisplayName("processReturn calls orderService and returns 200 OK")
    void testProcessReturn() throws Exception {
        UUID orderId = UUID.randomUUID();
        doNothing().when(orderService).processReturn(eq(orderId), eq(true));

        mockMvc.perform(put("/api/v1/orders/{id}/return/process", orderId)
                        .param("approved", "true"))
                .andExpect(status().isOk());

        verify(orderService).processReturn(eq(orderId), eq(true));
    }

    @Test
    @WithMockUser(roles = "SELLER")
    @DisplayName("completeReturn calls orderService and returns 200 OK")
    void testCompleteReturn() throws Exception {
        UUID orderId = UUID.randomUUID();
        doNothing().when(orderService).completeReturn(eq(orderId));

        mockMvc.perform(put("/api/v1/orders/{id}/return/complete", orderId))
                .andExpect(status().isOk());

        verify(orderService).completeReturn(eq(orderId));
    }

    @Test
    @WithMockUser(authorities = "order:read:my")
    @DisplayName("getMyOrders returns paginated user orders")
    void testGetMyOrders() throws Exception {
        OrderResponseDTO dto = new OrderResponseDTO(
                UUID.randomUUID(), UUID.randomUUID(), BigDecimal.valueOf(500),
                OrderStatus.DELIVERED, LocalDateTime.now(), Collections.emptyList(), null
        );
        Page<OrderResponseDTO> page = new PageImpl<>(List.of(dto));
        when(orderService.getMyOrders(any(Pageable.class))).thenReturn(page);

        mockMvc.perform(get("/api/v1/orders/my")
                        .param("page", "0")
                        .param("size", "10"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content.length()").value(1))
                .andExpect(jsonPath("$.content[0].id").value(dto.id().toString()))
                .andExpect(jsonPath("$.content[0].status").value("DELIVERED"));

        verify(orderService).getMyOrders(any(Pageable.class));
    }

    @Test
    @WithMockUser(authorities = "order:read:all")
    @DisplayName("getAllOrders returns paginated all orders")
    void testGetAllOrders() throws Exception {
        OrderResponseDTO dto = new OrderResponseDTO(
                UUID.randomUUID(), UUID.randomUUID(), BigDecimal.TEN,
                OrderStatus.CREATED, LocalDateTime.now(), Collections.emptyList(), null
        );
        Page<OrderResponseDTO> page = new PageImpl<>(List.of(dto));
        when(orderService.getAllOrders(any(Pageable.class))).thenReturn(page);

        mockMvc.perform(get("/api/v1/orders")
                        .param("page", "0")
                        .param("size", "10"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content.length()").value(1))
                .andExpect(jsonPath("$.content[0].id").value(dto.id().toString()));

        verify(orderService).getAllOrders(any(Pageable.class));
    }

    @Test
    @DisplayName("getOrderById returns specific order")
    void testGetOrderById() throws Exception {
        UUID orderId = UUID.randomUUID();
        OrderResponseDTO dto = new OrderResponseDTO(
                orderId, UUID.randomUUID(), BigDecimal.valueOf(250),
                OrderStatus.CONFIRMED, LocalDateTime.now(), Collections.emptyList(), null
        );
        when(orderService.getOrderById(orderId)).thenReturn(dto);

        mockMvc.perform(get("/api/v1/orders/{id}", orderId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(orderId.toString()))
                .andExpect(jsonPath("$.totalPrice").value(250));

        verify(orderService).getOrderById(orderId);
    }

    @Test
    @DisplayName("checkPurchase returns boolean flag")
    void testCheckPurchase() throws Exception {
        UUID productId = UUID.randomUUID();
        when(orderService.hasUserPurchasedProduct(productId)).thenReturn(true);

        mockMvc.perform(get("/api/v1/orders/check-purchase")
                        .param("productId", productId.toString()))
                .andExpect(status().isOk())
                .andExpect(content().string("true"));

        verify(orderService).hasUserPurchasedProduct(productId);
    }

    @Test
    @WithMockUser(roles = "SELLER")
    @DisplayName("getSellerOrders returns paginated seller orders")
    void testGetSellerOrders() throws Exception {
        OrderResponseDTO dto = new OrderResponseDTO(
                UUID.randomUUID(), UUID.randomUUID(), BigDecimal.valueOf(300),
                OrderStatus.PENDING_CONFIRMATION, LocalDateTime.now(), Collections.emptyList(), null
        );
        Page<OrderResponseDTO> page = new PageImpl<>(List.of(dto));
        when(orderService.getSellerOrders(eq(OrderStatus.PENDING_CONFIRMATION), any(Pageable.class))).thenReturn(page);

        mockMvc.perform(get("/api/v1/orders/seller")
                        .param("status", "PENDING_CONFIRMATION")
                        .param("page", "0")
                        .param("size", "10"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content.length()").value(1))
                .andExpect(jsonPath("$.content[0].status").value("PENDING_CONFIRMATION"));

        verify(orderService).getSellerOrders(eq(OrderStatus.PENDING_CONFIRMATION), any(Pageable.class));
    }

    @Test
    @WithMockUser(roles = "SELLER")
    @DisplayName("updateOrderStatus updates order status and returns updated order")
    void testUpdateOrderStatus() throws Exception {
        UUID orderId = UUID.randomUUID();
        OrderResponseDTO dto = new OrderResponseDTO(
                orderId, UUID.randomUUID(), BigDecimal.valueOf(400),
                OrderStatus.SHIPPED, LocalDateTime.now(), Collections.emptyList(), null
        );
        when(orderService.updateOrderStatusBySeller(orderId, OrderStatus.SHIPPED)).thenReturn(dto);

        mockMvc.perform(patch("/api/v1/orders/{id}/status", orderId)
                        .param("status", "SHIPPED"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(orderId.toString()))
                .andExpect(jsonPath("$.status").value("SHIPPED"));

        verify(orderService).updateOrderStatusBySeller(orderId, OrderStatus.SHIPPED);
    }

    @Test
    @DisplayName("checkSellerPurchase returns boolean flag")
    void testCheckSellerPurchase() throws Exception {
        UUID userId = UUID.randomUUID();
        UUID sellerId = UUID.randomUUID();
        when(orderService.hasUserBoughtFromSeller(userId, sellerId)).thenReturn(true);

        mockMvc.perform(get("/api/v1/orders/check-seller-purchase")
                        .param("userId", userId.toString())
                        .param("sellerId", sellerId.toString()))
                .andExpect(status().isOk())
                .andExpect(content().string("true"));

        verify(orderService).hasUserBoughtFromSeller(userId, sellerId);
    }
}
