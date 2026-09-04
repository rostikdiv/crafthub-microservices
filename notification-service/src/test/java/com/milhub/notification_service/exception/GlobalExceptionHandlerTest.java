package com.milhub.notification_service.exception;

import com.milhub.notification_service.dto.ErrorResponse;
import jakarta.servlet.http.HttpServletRequest;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class GlobalExceptionHandlerTest {

    private final GlobalExceptionHandler exceptionHandler = new GlobalExceptionHandler();

    @Test
    @DisplayName("handleGeneral returns 500 status and correct ErrorResponse body")
    void handleGeneral_ReturnsInternalServerError() {
        HttpServletRequest request = mock(HttpServletRequest.class);
        when(request.getRequestURI()).thenReturn("/notifications/send");

        Exception ex = new RuntimeException("Unexpected mail delivery failure");

        ResponseEntity<ErrorResponse> responseEntity = exceptionHandler.handleGeneral(ex, request);

        assertNotNull(responseEntity);
        assertEquals(HttpStatus.INTERNAL_SERVER_ERROR, responseEntity.getStatusCode());

        ErrorResponse body = responseEntity.getBody();
        assertNotNull(body);
        assertEquals(500, body.status());
        assertEquals("Internal Server Error", body.error());
        assertEquals("Unexpected mail delivery failure", body.message());
        assertEquals("/notifications/send", body.path());
        assertNotNull(body.timestamp());
        assertNull(body.validationErrors());
    }
}
