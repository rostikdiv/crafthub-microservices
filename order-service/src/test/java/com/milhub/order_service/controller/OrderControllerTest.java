package com.milhub.order_service.controller;

import com.milhub.order_service.dto.order.OrderResponseDTO;
import com.milhub.order_service.entity.OrderStatus;
import com.milhub.order_service.service.OrderService;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;

@WebMvcTest(OrderController.class)
@AutoConfigureMockMvc(addFilters = false) // Disable security filters for basic controller test
class OrderControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private OrderService orderService;

    @Test
    @org.springframework.security.test.context.support.WithMockUser(authorities = "order:read:all")
    void testGetAllOrders_WithPagination() throws Exception {
        // Arrange
        OrderResponseDTO dto = new OrderResponseDTO(
            UUID.randomUUID(), UUID.randomUUID(), BigDecimal.TEN, 
            OrderStatus.CREATED, 
            LocalDateTime.now(), java.util.Collections.emptyList(), null
        );
        Page<OrderResponseDTO> page = new PageImpl<>(List.of(dto));
        
        when(orderService.getAllOrders(any(org.springframework.data.domain.Pageable.class))).thenReturn(page);

        // Act & Assert
        mockMvc.perform(get("/api/v1/orders")
                .param("page", "0")
                .param("size", "10")
                .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content.length()").value(1))
                .andExpect(jsonPath("$.content[0].id").value(dto.id().toString()));
    }
}
