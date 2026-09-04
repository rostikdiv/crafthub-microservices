package com.milhub.order_service.exception;

import com.milhub.order_service.dto.ErrorResponse;
import jakarta.servlet.http.HttpServletRequest;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.BindingResult;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class GlobalExceptionHandlerTest {

    private final GlobalExceptionHandler exceptionHandler = new GlobalExceptionHandler();

    @Test
    @DisplayName("handleAppException builds proper response with custom status")
    void testHandleAppException() {
        HttpServletRequest request = mock(HttpServletRequest.class);
        when(request.getRequestURI()).thenReturn("/api/v1/orders/1");

        BusinessException ex = new BusinessException("Product out of stock");
        ResponseEntity<ErrorResponse> response = exceptionHandler.handleAppException(ex, request);

        assertEquals(HttpStatus.BAD_REQUEST, response.getStatusCode());
        assertNotNull(response.getBody());
        assertEquals("Product out of stock", response.getBody().message());
        assertEquals("/api/v1/orders/1", response.getBody().path());
        assertNull(response.getBody().errors());
    }

    @Test
    @DisplayName("handleValidation builds 400 response with field errors")
    void testHandleValidation() {
        HttpServletRequest request = mock(HttpServletRequest.class);
        when(request.getRequestURI()).thenReturn("/api/v1/orders");

        MethodArgumentNotValidException ex = mock(MethodArgumentNotValidException.class);
        BindingResult bindingResult = mock(BindingResult.class);
        when(ex.getBindingResult()).thenReturn(bindingResult);
        when(bindingResult.getFieldErrors()).thenReturn(List.of(
                new FieldError("orderRequestDTO", "items", "items must not be empty")
        ));

        ResponseEntity<ErrorResponse> response = exceptionHandler.handleValidation(ex, request);

        assertEquals(HttpStatus.BAD_REQUEST, response.getStatusCode());
        assertNotNull(response.getBody());
        assertEquals("Data validation error", response.getBody().message());
        assertTrue(response.getBody().errors().containsKey("items"));
    }

    @Test
    @DisplayName("handleAccessDenied handles Spring and custom AccessDeniedExceptions")
    void testHandleAccessDenied() {
        HttpServletRequest request = mock(HttpServletRequest.class);
        when(request.getRequestURI()).thenReturn("/api/v1/orders/cancel");

        AccessDeniedException customEx = new AccessDeniedException("Forbidden action");
        ResponseEntity<ErrorResponse> response = exceptionHandler.handleAccessDenied(customEx, request);

        assertEquals(HttpStatus.FORBIDDEN, response.getStatusCode());
        assertNotNull(response.getBody());
        assertTrue(response.getBody().message().contains("Access denied"));
    }

    @Test
    @DisplayName("handleGeneral handles generic exceptions returning 500")
    void testHandleGeneral() {
        HttpServletRequest request = mock(HttpServletRequest.class);
        when(request.getRequestURI()).thenReturn("/api/v1/orders");

        RuntimeException generalEx = new RuntimeException("Unexpected database glitch");
        ResponseEntity<ErrorResponse> response = exceptionHandler.handleGeneral(generalEx, request);

        assertEquals(HttpStatus.INTERNAL_SERVER_ERROR, response.getStatusCode());
        assertNotNull(response.getBody());
        assertTrue(response.getBody().message().contains("Unexpected database glitch"));
    }
}
