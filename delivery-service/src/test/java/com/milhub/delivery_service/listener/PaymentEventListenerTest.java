package com.milhub.delivery_service.listener;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.milhub.delivery_service.service.ShipmentService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Spy;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.UUID;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class PaymentEventListenerTest {

    @Mock
    private ShipmentService shipmentService;

    @Spy
    private ObjectMapper objectMapper = new ObjectMapper();

    @InjectMocks
    private PaymentEventListener paymentEventListener;

    @Test
    void handlePaymentSuccess_WhenValidMessage_ShouldCallCreateShipment() {
        UUID orderId = UUID.randomUUID();
        String message = String.format("{\"orderId\":\"%s\"}", orderId);

        paymentEventListener.handlePaymentSuccess(message);

        verify(shipmentService).createShipment(orderId);
    }

    @Test
    void handlePaymentSuccess_WhenInvalidJson_ShouldCatchExceptionAndNotThrow() {
        String invalidMessage = "{invalid_json}";

        paymentEventListener.handlePaymentSuccess(invalidMessage);

        verify(shipmentService, never()).createShipment(any());
    }

    @Test
    void handlePaymentSuccess_WhenShipmentServiceThrowsException_ShouldCatchAndNotPropagate() {
        UUID orderId = UUID.randomUUID();
        String message = String.format("{\"orderId\":\"%s\"}", orderId);

        doThrow(new RuntimeException("Order service unavailable")).when(shipmentService).createShipment(orderId);

        paymentEventListener.handlePaymentSuccess(message);

        verify(shipmentService).createShipment(orderId);
    }
}
