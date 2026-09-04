package com.milhub.delivery_service.exception;

import com.milhub.delivery_service.dto.ErrorResponse;
import jakarta.servlet.http.HttpServletRequest;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.validation.BindingResult;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class GlobalExceptionHandlerTest {

    private GlobalExceptionHandler handler;

    @Mock
    private HttpServletRequest request;

    @Mock
    private MethodArgumentNotValidException validationException;

    @Mock
    private BindingResult bindingResult;

    @BeforeEach
    void setUp() {
        handler = new GlobalExceptionHandler();
        when(request.getRequestURI()).thenReturn("/api/v1/delivery/test");
    }

    @Test
    void handleAppException_ResourceNotFound_ShouldReturn404() {
        ResourceNotFoundException ex = new ResourceNotFoundException("Not found item");

        ResponseEntity<ErrorResponse> response = handler.handleAppException(ex, request);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.NOT_FOUND);
        assertThat(response.getBody()).isNotNull();
        assertThat(response.getBody().message()).isEqualTo("Not found item");
        assertThat(response.getBody().path()).isEqualTo("/api/v1/delivery/test");
        assertThat(response.getBody().status()).isEqualTo(404);
    }

    @Test
    void handleAppException_BusinessException_ShouldReturn400() {
        BusinessException ex = new BusinessException("Invalid business operation");

        ResponseEntity<ErrorResponse> response = handler.handleAppException(ex, request);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
        assertThat(response.getBody()).isNotNull();
        assertThat(response.getBody().message()).isEqualTo("Invalid business operation");
        assertThat(response.getBody().status()).isEqualTo(400);
    }

    @Test
    void handleAccessDenied_ShouldReturn403() {
        AccessDeniedException ex = new AccessDeniedException("Forbidden");

        ResponseEntity<ErrorResponse> response = handler.handleAccessDenied(ex, request);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.FORBIDDEN);
        assertThat(response.getBody()).isNotNull();
        assertThat(response.getBody().message()).isEqualTo("Access Denied");
    }

    @Test
    void handleValidation_ShouldReturn400WithFieldErrors() {
        FieldError error1 = new FieldError("object", "trackingNumber", "must not be blank");
        when(validationException.getBindingResult()).thenReturn(bindingResult);
        when(bindingResult.getFieldErrors()).thenReturn(List.of(error1));

        ResponseEntity<ErrorResponse> response = handler.handleValidation(validationException, request);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
        assertThat(response.getBody()).isNotNull();
        assertThat(response.getBody().validationErrors()).containsEntry("trackingNumber", "must not be blank");
    }

    @Test
    void handleGeneral_ShouldReturn500() {
        RuntimeException ex = new RuntimeException("Unexpected db crash");

        ResponseEntity<ErrorResponse> response = handler.handleGeneral(ex, request);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.INTERNAL_SERVER_ERROR);
        assertThat(response.getBody()).isNotNull();
        assertThat(response.getBody().message()).contains("Internal Server Error: Unexpected db crash");
    }
}
