package com.milhub.notification_service;

import com.milhub.notification_service.dto.DeliveryStatusChangedEvent;
import com.milhub.notification_service.dto.ErrorResponse;
import com.milhub.notification_service.dto.OrderPlacedEventDTO;
import com.milhub.notification_service.dto.PaymentSuccessEventDTO;
import com.milhub.notification_service.dto.UserVerificationEvent;
import com.milhub.notification_service.exception.AppException;
import com.milhub.notification_service.exception.BusinessException;
import com.milhub.notification_service.exception.ResourceNotFoundException;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;

class NotificationEntityBranchTest {

    @Test
    @DisplayName("Test AppException constructors and getter")
    void testAppException() {
        AppException ex = new AppException("Custom app error", HttpStatus.BAD_GATEWAY);
        assertEquals("Custom app error", ex.getMessage());
        assertEquals(HttpStatus.BAD_GATEWAY, ex.getStatus());
    }

    @Test
    @DisplayName("Test BusinessException hierarchy and default status")
    void testBusinessException() {
        BusinessException ex = new BusinessException("Business constraint violated");
        assertEquals("Business constraint violated", ex.getMessage());
        assertEquals(HttpStatus.BAD_REQUEST, ex.getStatus());
        assertTrue(ex instanceof AppException);
    }

    @Test
    @DisplayName("Test ResourceNotFoundException hierarchy and default status")
    void testResourceNotFoundException() {
        ResourceNotFoundException ex = new ResourceNotFoundException("Notification template not found");
        assertEquals("Notification template not found", ex.getMessage());
        assertEquals(HttpStatus.NOT_FOUND, ex.getStatus());
        assertTrue(ex instanceof AppException);
    }

    @Test
    @DisplayName("Test ErrorResponse record methods")
    void testErrorResponseRecord() {
        LocalDateTime now = LocalDateTime.now();
        Map<String, String> errors = Map.of("field", "error");
        ErrorResponse res1 = new ErrorResponse(now, 400, "Bad Request", "Validation failed", "/api/test", errors);
        ErrorResponse res2 = new ErrorResponse(now, 400, "Bad Request", "Validation failed", "/api/test", errors);

        assertEquals(now, res1.timestamp());
        assertEquals(400, res1.status());
        assertEquals("Bad Request", res1.error());
        assertEquals("Validation failed", res1.message());
        assertEquals("/api/test", res1.path());
        assertEquals(errors, res1.validationErrors());
        assertEquals(res1, res2);
        assertEquals(res1.hashCode(), res2.hashCode());
        assertTrue(res1.toString().contains("ErrorResponse"));
    }

    @Test
    @DisplayName("Test DeliveryStatusChangedEvent record methods")
    void testDeliveryStatusChangedEvent() {
        UUID orderId = UUID.randomUUID();
        DeliveryStatusChangedEvent event1 = new DeliveryStatusChangedEvent(orderId, "DELIVERED", "TRK-001");
        DeliveryStatusChangedEvent event2 = new DeliveryStatusChangedEvent(orderId, "DELIVERED", "TRK-001");

        assertEquals(orderId, event1.orderId());
        assertEquals("DELIVERED", event1.status());
        assertEquals("TRK-001", event1.trackingNumber());
        assertEquals(event1, event2);
        assertEquals(event1.hashCode(), event2.hashCode());
        assertTrue(event1.toString().contains("DeliveryStatusChangedEvent"));
    }

    @Test
    @DisplayName("Test OrderPlacedEventDTO record methods")
    void testOrderPlacedEventDTO() {
        UUID orderId = UUID.randomUUID();
        UUID userId = UUID.randomUUID();
        UUID prodId = UUID.randomUUID();
        OrderPlacedEventDTO dto1 = new OrderPlacedEventDTO(orderId, userId, "user@test.com", BigDecimal.TEN, "Product X", List.of(prodId));
        OrderPlacedEventDTO dto2 = new OrderPlacedEventDTO(orderId, userId, "user@test.com", BigDecimal.TEN, "Product X", List.of(prodId));

        assertEquals(orderId, dto1.orderId());
        assertEquals(userId, dto1.userId());
        assertEquals("user@test.com", dto1.userEmail());
        assertEquals(BigDecimal.TEN, dto1.totalPrice());
        assertEquals("Product X", dto1.productName());
        assertEquals(List.of(prodId), dto1.productIds());
        assertEquals(dto1, dto2);
        assertEquals(dto1.hashCode(), dto2.hashCode());
        assertTrue(dto1.toString().contains("OrderPlacedEventDTO"));
    }

    @Test
    @DisplayName("Test PaymentSuccessEventDTO record methods")
    void testPaymentSuccessEventDTO() {
        UUID orderId = UUID.randomUUID();
        PaymentSuccessEventDTO dto1 = new PaymentSuccessEventDTO(orderId, "payer@test.com", BigDecimal.valueOf(99.99));
        PaymentSuccessEventDTO dto2 = new PaymentSuccessEventDTO(orderId, "payer@test.com", BigDecimal.valueOf(99.99));

        assertEquals(orderId, dto1.orderId());
        assertEquals("payer@test.com", dto1.userEmail());
        assertEquals(BigDecimal.valueOf(99.99), dto1.amount());
        assertEquals(dto1, dto2);
        assertEquals(dto1.hashCode(), dto2.hashCode());
        assertTrue(dto1.toString().contains("PaymentSuccessEventDTO"));
    }

    @Test
    @DisplayName("Test UserVerificationEvent record methods")
    void testUserVerificationEvent() {
        UUID userId = UUID.randomUUID();
        UserVerificationEvent event1 = new UserVerificationEvent(userId, "seller@test.com", true, "Verified by Admin");
        UserVerificationEvent event2 = new UserVerificationEvent(userId, "seller@test.com", true, "Verified by Admin");

        assertEquals(userId, event1.userId());
        assertEquals("seller@test.com", event1.email());
        assertTrue(event1.isVerified());
        assertEquals("Verified by Admin", event1.reason());
        assertEquals(event1, event2);
        assertEquals(event1.hashCode(), event2.hashCode());
        assertTrue(event1.toString().contains("UserVerificationEvent"));
    }
}
