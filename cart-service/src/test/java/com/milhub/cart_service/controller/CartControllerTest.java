package com.milhub.cart_service.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.milhub.cart_service.dto.CartItemRequestDTO;
import com.milhub.cart_service.entity.Cart;
import com.milhub.cart_service.entity.CartItem;
import com.milhub.cart_service.entity.CartSection;
import com.milhub.cart_service.service.CartService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("CartController Unit Tests")
class CartControllerTest {

    private MockMvc mockMvc;
    private ObjectMapper objectMapper;

    @Mock
    private CartService cartService;

    @InjectMocks
    private CartController cartController;

    private UUID userId;
    private UUID productId;
    private UUID sellerId;

    @BeforeEach
    void setUp() {
        mockMvc = MockMvcBuilders.standaloneSetup(cartController).build();
        objectMapper = new ObjectMapper();
        userId = UUID.randomUUID();
        productId = UUID.randomUUID();
        sellerId = UUID.randomUUID();
    }

    @Test
    void testGetCart() throws Exception {
        Cart cart = Cart.builder()
                .userId(userId)
                .sections(List.of(new CartSection(sellerId, "Seller", "logo", List.of(
                        new CartItem(productId, "Product", "img", 1, BigDecimal.TEN)
                ))))
                .totalPrice(BigDecimal.TEN)
                .build();

        when(cartService.getMyCart()).thenReturn(cart);

        mockMvc.perform(get("/api/v1/cart"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.userId").value(userId.toString()))
                .andExpect(jsonPath("$.totalPrice").value(10));
    }

    @Test
    void testAddItem() throws Exception {
        CartItemRequestDTO dto = new CartItemRequestDTO(productId, 2);
        Cart cart = Cart.builder().userId(userId).totalPrice(BigDecimal.valueOf(20)).build();

        when(cartService.addItemToMyCart(any(CartItemRequestDTO.class))).thenReturn(cart);

        mockMvc.perform(post("/api/v1/cart/items")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(dto)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.userId").value(userId.toString()))
                .andExpect(jsonPath("$.totalPrice").value(20));
    }

    @Test
    void testRemoveItem() throws Exception {
        Cart cart = Cart.builder().userId(userId).totalPrice(BigDecimal.ZERO).build();

        when(cartService.removeItemFromMyCart(eq(productId.toString()))).thenReturn(cart);

        mockMvc.perform(delete("/api/v1/cart/items/{productId}", productId.toString()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.userId").value(userId.toString()));
    }

    @Test
    void testClearCart() throws Exception {
        doNothing().when(cartService).clearMyCart();

        mockMvc.perform(delete("/api/v1/cart"))
                .andExpect(status().isNoContent());

        verify(cartService, times(1)).clearMyCart();
    }

    @Test
    void testTestEndpoint() throws Exception {
        mockMvc.perform(get("/api/v1/cart/test"))
                .andExpect(status().isOk())
                .andExpect(content().string("cart service works!"));
    }
}
