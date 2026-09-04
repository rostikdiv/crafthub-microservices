package com.milhub.notification_service.listeners;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.milhub.notification_service.dto.DeliveryStatusChangedEvent;
import com.milhub.notification_service.dto.OrderPlacedEventDTO;
import com.milhub.notification_service.dto.PaymentSuccessEventDTO;
import com.milhub.notification_service.dto.UserVerificationEvent;
import com.milhub.notification_service.service.EmailService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class KafkaListenersTest {

    @Mock
    private EmailService emailService;

    private ObjectMapper objectMapper;
    private KafkaListeners kafkaListeners;

    @BeforeEach
    void setUp() {
        objectMapper = new ObjectMapper();
        kafkaListeners = new KafkaListeners(emailService, objectMapper);
    }

    @Test
    @DisplayName("handleOrderPlaced should send confirmation when email is provided")
    void handleOrderPlaced_ShouldCallEmailService() throws Exception {
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

        kafkaListeners.handleOrderPlaced(message);

        verify(emailService, times(1)).sendOrderConfirmation("test@user.com", orderId.toString(), BigDecimal.valueOf(100), "Product A");
    }

    @Test
    @DisplayName("handleOrderPlaced should fallback to default email when userEmail is null")
    void handleOrderPlaced_WithNullEmail_FallbackDefault() throws Exception {
        UUID orderId = UUID.randomUUID();
        OrderPlacedEventDTO event = new OrderPlacedEventDTO(
                orderId,
                UUID.randomUUID(),
                null,
                BigDecimal.valueOf(100),
                "Product B",
                List.of()
        );
        String message = objectMapper.writeValueAsString(event);

        kafkaListeners.handleOrderPlaced(message);

        verify(emailService, times(1)).sendOrderConfirmation("unknown@user.com", orderId.toString(), BigDecimal.valueOf(100), "Product B");
    }

    @Test
    @DisplayName("handleOrderPlaced should handle double-encoded JSON strings")
    void handleOrderPlaced_DoubleEncodedJson() throws Exception {
        UUID orderId = UUID.randomUUID();
        OrderPlacedEventDTO event = new OrderPlacedEventDTO(
                orderId,
                UUID.randomUUID(),
                "double@user.com",
                BigDecimal.valueOf(50),
                "Product C",
                List.of()
        );
        String rawJson = objectMapper.writeValueAsString(event);
        String doubleEncoded = objectMapper.writeValueAsString(rawJson);

        kafkaListeners.handleOrderPlaced(doubleEncoded);

        verify(emailService, times(1)).sendOrderConfirmation("double@user.com", orderId.toString(), BigDecimal.valueOf(50), "Product C");
    }

    @Test
    @DisplayName("handleOrderPlaced should gracefully catch and log parse exceptions")
    void handleOrderPlaced_InvalidPayload_ExceptionCaught() {
        kafkaListeners.handleOrderPlaced("invalid-json");

        verifyNoInteractions(emailService);
    }

    @Test
    @DisplayName("handleOrderPlaced should handle null and blank messages")
    void handleOrderPlaced_NullOrBlankPayload() {
        kafkaListeners.handleOrderPlaced(null);
        kafkaListeners.handleOrderPlaced("   ");

        verifyNoInteractions(emailService);
    }

    @Test
    @DisplayName("handlePaymentSuccess should send payment success email when userEmail is provided")
    void handlePaymentSuccess_ShouldCallEmailService() throws Exception {
        UUID orderId = UUID.randomUUID();
        PaymentSuccessEventDTO event = new PaymentSuccessEventDTO(orderId, "test@user.com", BigDecimal.valueOf(200));
        String message = objectMapper.writeValueAsString(event);

        kafkaListeners.handlePaymentSuccess(message);

        verify(emailService, times(1)).sendPaymentSuccess("test@user.com", orderId.toString(), BigDecimal.valueOf(200));
    }

    @Test
    @DisplayName("handlePaymentSuccess should fallback to default email when userEmail is null")
    void handlePaymentSuccess_NullEmail_FallbackDefault() throws Exception {
        UUID orderId = UUID.randomUUID();
        PaymentSuccessEventDTO event = new PaymentSuccessEventDTO(orderId, null, BigDecimal.valueOf(300));
        String message = objectMapper.writeValueAsString(event);

        kafkaListeners.handlePaymentSuccess(message);

        verify(emailService, times(1)).sendPaymentSuccess("user@example.com", orderId.toString(), BigDecimal.valueOf(300));
    }

    @Test
    @DisplayName("handlePaymentSuccess should gracefully catch and log exceptions")
    void handlePaymentSuccess_ExceptionCaught() {
        kafkaListeners.handlePaymentSuccess("malformed");

        verifyNoInteractions(emailService);
    }

    @Test
    @DisplayName("handleDeliveryUpdate should call emailService with delivery info")
    void handleDeliveryUpdate_ShouldCallEmailService() throws Exception {
        UUID orderId = UUID.randomUUID();
        DeliveryStatusChangedEvent event = new DeliveryStatusChangedEvent(orderId, "SHIPPED", "TRACK-123");
        String message = objectMapper.writeValueAsString(event);

        kafkaListeners.handleDeliveryUpdate(message);

        verify(emailService, times(1)).sendDeliveryUpdate("user@example.com", orderId.toString(), "SHIPPED");
    }

    @Test
    @DisplayName("handleDeliveryUpdate should gracefully catch and log exceptions")
    void handleDeliveryUpdate_ExceptionCaught() {
        kafkaListeners.handleDeliveryUpdate("{invalid json}");

        verifyNoInteractions(emailService);
    }

    @Test
    @DisplayName("handleUserVerification should send approved email when verified is true")
    void handleUserVerification_WhenVerified_ShouldSendApprovedEmail() throws Exception {
        UserVerificationEvent event = new UserVerificationEvent(UUID.randomUUID(), "seller@test.com", true, null);
        String message = objectMapper.writeValueAsString(event);

        kafkaListeners.handleUserVerification(message);

        verify(emailService, times(1)).sendVerificationApproved("seller@test.com");
        verify(emailService, never()).sendVerificationRejected(anyString(), any());
    }

    @Test
    @DisplayName("handleUserVerification should send rejected email when verified is false")
    void handleUserVerification_WhenRejected_ShouldSendRejectedEmail() throws Exception {
        UserVerificationEvent event = new UserVerificationEvent(UUID.randomUUID(), "seller@test.com", false, "Bad ID photo");
        String message = objectMapper.writeValueAsString(event);

        kafkaListeners.handleUserVerification(message);

        verify(emailService, times(1)).sendVerificationRejected("seller@test.com", "Bad ID photo");
        verify(emailService, never()).sendVerificationApproved(anyString());
    }

    @Test
    @DisplayName("handleUserVerification should gracefully catch and log exceptions")
    void handleUserVerification_ExceptionCaught() {
        kafkaListeners.handleUserVerification("");

        verifyNoInteractions(emailService);
    }

    @Test
    @DisplayName("parseMessage inner exception ignored when double quote readValue fails")
    void parseMessage_DoubleQuoteFailureIgnored() throws Exception {
        ObjectMapper mockMapper = mock(ObjectMapper.class);
        KafkaListeners listenersWithMock = new KafkaListeners(emailService, mockMapper);

        // String starts and ends with quotes and length > 2
        String raw = "\"some-bad-json\"";
        when(mockMapper.readValue(raw, String.class)).thenThrow(new JsonProcessingException("Inner error") {});
        when(mockMapper.readValue(raw, UserVerificationEvent.class)).thenThrow(new JsonProcessingException("Outer error") {});

        listenersWithMock.handleUserVerification(raw);

        verify(mockMapper).readValue(raw, String.class);
        verify(mockMapper).readValue(raw, UserVerificationEvent.class);
    }

    @Test
    @DisplayName("parseMessage handles quotes that do not match or have length <= 2")
    void parseMessage_EdgeCaseQuotes() {
        // Starts with quote but doesn't end with quote
        kafkaListeners.handleOrderPlaced("\"unclosed json");

        // Starts and ends with quote but length == 2
        kafkaListeners.handleOrderPlaced("\"\"");

        verifyNoInteractions(emailService);
    }
}
