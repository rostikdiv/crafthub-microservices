package com.milhub.cart_service.listener;

import com.milhub.cart_service.dto.OrderPlacedEventDTO;
import com.milhub.cart_service.service.CartService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.util.Collections;
import java.util.List;
import java.util.UUID;

import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("CartKafkaListener Unit Tests")
class CartKafkaListenerTest {

    @Mock
    private CartService cartService;

    @InjectMocks
    private CartKafkaListener listener;

    private UUID userId;
    private UUID product1;
    private UUID product2;

    @BeforeEach
    void setUp() {
        userId = UUID.randomUUID();
        product1 = UUID.randomUUID();
        product2 = UUID.randomUUID();
    }

    @Test
    void testHandleOrderPlaced_Success() {
        OrderPlacedEventDTO event = new OrderPlacedEventDTO(UUID.randomUUID(), userId, "user@milhub.ua", BigDecimal.valueOf(100), "Prod", List.of(product1, product2));

        listener.handleOrderPlaced(event);

        verify(cartService, times(1)).removeItemFromCart(userId, product1.toString());
        verify(cartService, times(1)).removeItemFromCart(userId, product2.toString());
    }

    @Test
    void testHandleOrderPlaced_ServiceThrowsException_HandledGracefully() {
        OrderPlacedEventDTO event = new OrderPlacedEventDTO(UUID.randomUUID(), userId, "user@milhub.ua", BigDecimal.valueOf(100), "Prod", List.of(product1, product2));

        doThrow(new RuntimeException("Cart not found")).when(cartService).removeItemFromCart(userId, product1.toString());

        listener.handleOrderPlaced(event);

        verify(cartService, times(1)).removeItemFromCart(userId, product1.toString());
        verify(cartService, times(1)).removeItemFromCart(userId, product2.toString());
    }

    @Test
    void testHandleOrderPlaced_NullProductIds_Skipped() {
        OrderPlacedEventDTO event = new OrderPlacedEventDTO(UUID.randomUUID(), userId, "user@milhub.ua", BigDecimal.valueOf(100), "Prod", null);

        listener.handleOrderPlaced(event);

        verifyNoInteractions(cartService);
    }

    @Test
    void testHandleOrderPlaced_EmptyProductIds_Skipped() {
        OrderPlacedEventDTO event = new OrderPlacedEventDTO(UUID.randomUUID(), userId, "user@milhub.ua", BigDecimal.valueOf(100), "Prod", Collections.emptyList());

        listener.handleOrderPlaced(event);

        verifyNoInteractions(cartService);
    }
}
