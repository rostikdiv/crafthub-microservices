package com.milhub.payment_service.exception;

import com.milhub.payment_service.dto.ErrorResponse;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.security.access.AccessDeniedException;

import static org.junit.jupiter.api.Assertions.*;

@DisplayName("GlobalExceptionHandler Unit Tests")
class GlobalExceptionHandlerTest {

    private GlobalExceptionHandler handler;
    private MockHttpServletRequest request;

    @BeforeEach
    void setUp() {
        handler = new GlobalExceptionHandler();
        request = new MockHttpServletRequest();
        request.setRequestURI("/api/v1/payments/init");
    }

    @Test
    void testHandleAppException_ResourceNotFound() {
        ResourceNotFoundException ex = new ResourceNotFoundException("Transaction not found");

        ResponseEntity<ErrorResponse> response = handler.handleAppException(ex, request);

        assertEquals(HttpStatus.NOT_FOUND, response.getStatusCode());
        assertNotNull(response.getBody());
        assertEquals("Transaction not found", response.getBody().message());
        assertEquals("/api/v1/payments/init", response.getBody().path());
    }

    @Test
    void testHandleAppException_BusinessException() {
        BusinessException ex = new BusinessException("Payment failed");

        ResponseEntity<ErrorResponse> response = handler.handleAppException(ex, request);

        assertEquals(HttpStatus.CONFLICT, response.getStatusCode());
        assertNotNull(response.getBody());
        assertEquals("Payment failed", response.getBody().message());
    }

    @Test
    void testHandleAccessDenied() {
        AccessDeniedException ex = new AccessDeniedException("Forbidden");

        ResponseEntity<ErrorResponse> response = handler.handleAccessDenied(ex, request);

        assertEquals(HttpStatus.FORBIDDEN, response.getStatusCode());
        assertNotNull(response.getBody());
        assertEquals("Access Denied", response.getBody().message());
    }

    @Test
    void testHandleGeneral() {
        RuntimeException ex = new RuntimeException("Unexpected SQL error");

        ResponseEntity<ErrorResponse> response = handler.handleGeneral(ex, request);

        assertEquals(HttpStatus.INTERNAL_SERVER_ERROR, response.getStatusCode());
        assertNotNull(response.getBody());
        assertTrue(response.getBody().message().contains("Unexpected SQL error"));
    }
}
