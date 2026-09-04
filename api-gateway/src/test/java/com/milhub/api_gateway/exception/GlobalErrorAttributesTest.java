package com.milhub.api_gateway.exception;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.boot.web.error.ErrorAttributeOptions;
import org.springframework.http.HttpStatus;
import org.springframework.mock.http.server.reactive.MockServerHttpRequest;
import org.springframework.mock.web.reactive.function.server.MockServerRequest;
import org.springframework.mock.web.server.MockServerWebExchange;
import org.springframework.web.reactive.function.server.ServerRequest;
import org.springframework.web.server.ResponseStatusException;

import java.net.ConnectException;
import java.net.URI;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@DisplayName("GlobalErrorAttributes Tests")
class GlobalErrorAttributesTest {

    private ServerRequest createMockRequest(String path) {
        MockServerHttpRequest httpRequest = MockServerHttpRequest.get(path).build();
        MockServerWebExchange exchange = MockServerWebExchange.from(httpRequest);
        return MockServerRequest.builder()
                .exchange(exchange)
                .uri(URI.create("http://localhost:8080" + path))
                .build();
    }

    @Test
    void testGetErrorAttributes_ResponseStatusException() {
        GlobalErrorAttributes attributes = spy(new GlobalErrorAttributes());
        ServerRequest request = createMockRequest("/api/v1/orders/999");
        ResponseStatusException ex = new ResponseStatusException(HttpStatus.NOT_FOUND, "Order not found");

        doReturn(ex).when(attributes).getError(request);

        Map<String, Object> result = attributes.getErrorAttributes(request, ErrorAttributeOptions.defaults());

        assertEquals(404, result.get("status"));
        assertEquals("/api/v1/orders/999", result.get("path"));
        assertTrue(result.containsKey("timestamp"));
        assertNull(result.get("requestId"));
        assertNull(result.get("trace"));
    }

    @Test
    void testGetErrorAttributes_UnauthorizedMessage() {
        GlobalErrorAttributes attributes = spy(new GlobalErrorAttributes());
        ServerRequest request = createMockRequest("/api/v1/admin");
        RuntimeException ex = new RuntimeException("Unauthorized access to admin resource");

        doReturn(ex).when(attributes).getError(request);

        Map<String, Object> result = attributes.getErrorAttributes(request, ErrorAttributeOptions.defaults());

        assertEquals(HttpStatus.UNAUTHORIZED.value(), result.get("status"));
        assertEquals("Unauthorized", result.get("error"));
        assertEquals("Unauthorized access to admin resource", result.get("message"));
    }

    @Test
    void testGetErrorAttributes_JwtTokenMessage() {
        GlobalErrorAttributes attributes = spy(new GlobalErrorAttributes());
        ServerRequest request = createMockRequest("/api/v1/cart");
        RuntimeException ex = new RuntimeException("Invalid Jwt signature");

        doReturn(ex).when(attributes).getError(request);

        Map<String, Object> result = attributes.getErrorAttributes(request, ErrorAttributeOptions.defaults());

        assertEquals(HttpStatus.UNAUTHORIZED.value(), result.get("status"));
        assertEquals("Unauthorized", result.get("error"));
        assertEquals("Invalid Jwt signature", result.get("message"));
    }

    @Test
    void testGetErrorAttributes_ConnectException() {
        GlobalErrorAttributes attributes = spy(new GlobalErrorAttributes());
        ServerRequest request = createMockRequest("/api/v1/delivery");
        ConnectException ex = new ConnectException("Connection refused: no further information");

        doReturn(ex).when(attributes).getError(request);

        Map<String, Object> result = attributes.getErrorAttributes(request, ErrorAttributeOptions.defaults());

        assertEquals(HttpStatus.SERVICE_UNAVAILABLE.value(), result.get("status"));
        assertEquals("Service Unavailable", result.get("error"));
        assertEquals("Microservice is down or unreachable", result.get("message"));
    }

    @Test
    void testGetErrorAttributes_GenericException_NullMessage() {
        GlobalErrorAttributes attributes = spy(new GlobalErrorAttributes());
        ServerRequest request = createMockRequest("/api/v1/products");
        RuntimeException ex = new NullPointerException();

        doReturn(ex).when(attributes).getError(request);

        Map<String, Object> result = attributes.getErrorAttributes(request, ErrorAttributeOptions.defaults());

        assertEquals(500, result.get("status"));
        assertNull(result.get("message"));
    }

    @Test
    void testGetErrorAttributes_GenericException_NonNullMessage() {
        GlobalErrorAttributes attributes = spy(new GlobalErrorAttributes());
        ServerRequest request = createMockRequest("/api/v1/products");
        RuntimeException ex = new IllegalArgumentException("Invalid category parameter");

        doReturn(ex).when(attributes).getError(request);

        Map<String, Object> result = attributes.getErrorAttributes(request, ErrorAttributeOptions.defaults());

        assertEquals(500, result.get("status"));
        assertEquals("Invalid category parameter", result.get("message"));
    }
}
