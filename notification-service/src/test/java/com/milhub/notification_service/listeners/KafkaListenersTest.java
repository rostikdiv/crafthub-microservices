package com.milhub.notification_service.listeners;

import com.milhub.notification_service.dto.DeliveryStatusChangedEvent;
import com.milhub.notification_service.dto.OrderPlacedEventDTO;
import com.milhub.notification_service.dto.PaymentSuccessEventDTO;
import com.milhub.notification_service.dto.UserVerificationEvent;
import com.milhub.notification_service.service.EmailService;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;

import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class KafkaListenersTest {

    @Mock
    private EmailService emailService;

    @Mock
    private ObjectMapper objectMapper;

    @InjectMocks
    private KafkaListeners kafkaListeners;

    @BeforeEach
    void setUp() {
        objectMapper = new ObjectMapper();
        kafkaListeners = new KafkaListeners(emailService, objectMapper);
    }

    @Test
    void handleOrderPlaced_ShouldCallEmailService() throws Exception {
        // Arrange
        UUID orderId = UUID.randomUUID();
        OrderPlacedEventDTO event = new OrderPlacedEventDTO(
                orderId,
                UUID.randomUUID(),
                "test@user.com",
                BigDecimal.valueOf(100),
                "Product A",
                List.of(UUID.randomUUID())
        );
        String message = objectMapper.writeValueAsString(event);

        // Act
        kafkaListeners.handleOrderPlaced(message);

        // Assert
        verify(emailService, times(1)).sendOrderConfirmation("test@user.com", orderId.toString(), BigDecimal.valueOf(100), "Product A");
    }

    @Test
    void handlePaymentSuccess_ShouldCallEmailService() throws Exception {
        // Arrange
        UUID orderId = UUID.randomUUID();
        PaymentSuccessEventDTO event = new PaymentSuccessEventDTO(orderId, "test@user.com", BigDecimal.valueOf(200));
        String message = objectMapper.writeValueAsString(event);

        // Act
        kafkaListeners.handlePaymentSuccess(message);

        // Assert
        verify(emailService, times(1)).sendPaymentSuccess("test@user.com", orderId.toString(), BigDecimal.valueOf(200));
    }

    @Test
    void handleDeliveryUpdate_ShouldCallEmailService() throws Exception {
        // Arrange
        UUID orderId = UUID.randomUUID();
        DeliveryStatusChangedEvent event = new DeliveryStatusChangedEvent(orderId, "SHIPPED", "TRACK-123");
        String message = objectMapper.writeValueAsString(event);

        // Act
        kafkaListeners.handleDeliveryUpdate(message);

        // Assert
        verify(emailService, times(1)).sendDeliveryUpdate("user@example.com", orderId.toString(), "SHIPPED");
    }

    @Test
    void handleUserVerification_WhenVerified_ShouldSendApprovedEmail() throws Exception {
        // Arrange
        UserVerificationEvent event = new UserVerificationEvent(UUID.randomUUID(), "seller@test.com", true, null);
        String message = objectMapper.writeValueAsString(event);

        // Act
        kafkaListeners.handleUserVerification(message);

        // Assert
        verify(emailService, times(1)).sendVerificationApproved("seller@test.com");
        verify(emailService, never()).sendVerificationRejected(anyString(), any());
    }

    @Test
    void handleUserVerification_WhenRejected_ShouldSendRejectedEmail() throws Exception {
        // Arrange
        UserVerificationEvent event = new UserVerificationEvent(UUID.randomUUID(), "seller@test.com", false, "Bad ID photo");
        String message = objectMapper.writeValueAsString(event);

        // Act
        kafkaListeners.handleUserVerification(message);

        // Assert
        verify(emailService, times(1)).sendVerificationRejected("seller@test.com", "Bad ID photo");
        verify(emailService, never()).sendVerificationApproved(anyString());
    }
}
