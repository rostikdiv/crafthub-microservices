package com.milhub.order_service.listener;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import com.milhub.order_service.dto.event.DeliveryStatusChangedEvent;
import com.milhub.order_service.dto.event.PaymentSuccessEventDTO;
import com.milhub.order_service.service.OrderService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class KafkaListenerTest {

    @Mock
    private OrderService orderService;

    private ObjectMapper objectMapper;

    @BeforeEach
    void setUp() {
        objectMapper = new ObjectMapper();
        objectMapper.registerModule(new JavaTimeModule());
    }

    @Test
    @DisplayName("DeliveryStatusListener successfully parses event and updates order status")
    void deliveryStatusListener_Success() throws Exception {
        DeliveryStatusListener listener = new DeliveryStatusListener(orderService, objectMapper);
        UUID orderId = UUID.randomUUID();
        DeliveryStatusChangedEvent event = new DeliveryStatusChangedEvent(orderId, "DELIVERED", LocalDateTime.now());
        String message = objectMapper.writeValueAsString(event);

        listener.handleDeliveryStatusChange(message);

        verify(orderService).updateOrderStatusFromDelivery(orderId, "DELIVERED");
    }

    @Test
    @DisplayName("DeliveryStatusListener handles malformed JSON without crashing")
    void deliveryStatusListener_MalformedJson_HandledGracefully() {
        DeliveryStatusListener listener = new DeliveryStatusListener(orderService, objectMapper);

        assertDoesNotThrow(() -> listener.handleDeliveryStatusChange("invalid json content"));
        verifyNoInteractions(orderService);
    }

    @Test
    @DisplayName("PaymentEventListener successfully parses event and confirms payment")
    void paymentEventListener_Success() throws Exception {
        PaymentEventListener listener = new PaymentEventListener(orderService, objectMapper);
        UUID orderId = UUID.randomUUID();
        PaymentSuccessEventDTO event = new PaymentSuccessEventDTO(orderId, "user@milhub.com", BigDecimal.valueOf(350));
        String message = objectMapper.writeValueAsString(event);

        listener.handlePaymentSuccess(message);

        verify(orderService).confirmOrderPayment(orderId);
    }

    @Test
    @DisplayName("PaymentEventListener handles malformed JSON without crashing")
    void paymentEventListener_MalformedJson_HandledGracefully() {
        PaymentEventListener listener = new PaymentEventListener(orderService, objectMapper);

        assertDoesNotThrow(() -> listener.handlePaymentSuccess("corrupt message"));
        verifyNoInteractions(orderService);
    }
}
