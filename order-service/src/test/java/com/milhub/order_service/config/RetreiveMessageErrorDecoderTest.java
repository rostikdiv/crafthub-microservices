package com.milhub.order_service.config;

import com.milhub.order_service.exception.BusinessException;
import com.milhub.order_service.exception.ResourceNotFoundException;
import feign.Request;
import feign.Response;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.Collections;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class RetreiveMessageErrorDecoderTest {

    private final RetreiveMessageErrorDecoder decoder = new RetreiveMessageErrorDecoder();

    private Response createMockResponse(int status, String bodyText) {
        Response.Body body = null;
        if (bodyText != null) {
            byte[] bytes = bodyText.getBytes(StandardCharsets.UTF_8);
            body = mock(Response.Body.class);
            try {
                when(body.asInputStream()).thenReturn(new ByteArrayInputStream(bytes));
            } catch (IOException ignored) {}
        }

        return Response.builder()
                .status(status)
                .reason("Reason")
                .request(Request.create(Request.HttpMethod.GET, "/api/test", Collections.emptyMap(), null, StandardCharsets.UTF_8, null))
                .headers(Collections.emptyMap())
                .body(body)
                .build();
    }

    @Test
    @DisplayName("decode maps 400 to BusinessException with message")
    void decode_400_MapsToBusinessException() {
        Response response = createMockResponse(400, "Insufficient stock for product");
        Exception ex = decoder.decode("ProductClient#reduceStock", response);

        assertInstanceOf(BusinessException.class, ex);
        assertEquals("Insufficient stock for product", ex.getMessage());
    }

    @Test
    @DisplayName("decode maps 404 to ResourceNotFoundException with message")
    void decode_404_MapsToResourceNotFoundException() {
        Response response = createMockResponse(404, "Product not found");
        Exception ex = decoder.decode("ProductClient#getProduct", response);

        assertInstanceOf(ResourceNotFoundException.class, ex);
        assertEquals("Product not found", ex.getMessage());
    }

    @Test
    @DisplayName("decode falls back to default error message when body is empty")
    void decode_EmptyBody_DefaultMessage() {
        Response responseEmpty = createMockResponse(400, "");
        Exception ex1 = decoder.decode("ProductClient#reduceStock", responseEmpty);
        assertInstanceOf(BusinessException.class, ex1);
        assertEquals("Unknown error from external service", ex1.getMessage());
    }

    @Test
    @DisplayName("decode handles IOException from body stream")
    void decode_IOException_Handled() throws IOException {
        Response.Body body = mock(Response.Body.class);
        when(body.asInputStream()).thenThrow(new IOException("Stream failed"));

        Response response = Response.builder()
                .status(400)
                .reason("Bad Request")
                .request(Request.create(Request.HttpMethod.GET, "/api/test", Collections.emptyMap(), null, StandardCharsets.UTF_8, null))
                .headers(Collections.emptyMap())
                .body(body)
                .build();

        Exception ex = decoder.decode("ProductClient#reduceStock", response);
        assertInstanceOf(BusinessException.class, ex);
        assertEquals("Failed to process error response", ex.getMessage());
    }

    @Test
    @DisplayName("decode maps 500 to default FeignException")
    void decode_500_DefaultDecoder() {
        Response response = createMockResponse(500, "Internal Server Error");
        Exception ex = decoder.decode("ProductClient#reduceStock", response);

        assertNotNull(ex);
        assertFalse(ex instanceof BusinessException);
        assertFalse(ex instanceof ResourceNotFoundException);
    }
}
