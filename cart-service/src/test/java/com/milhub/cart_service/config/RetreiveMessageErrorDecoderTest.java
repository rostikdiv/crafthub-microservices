package com.milhub.cart_service.config;

import com.milhub.cart_service.exception.BusinessException;
import com.milhub.cart_service.exception.ResourceNotFoundException;
import feign.Request;
import feign.Response;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.Collections;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@DisplayName("RetreiveMessageErrorDecoder Unit Tests")
class RetreiveMessageErrorDecoderTest {

    private RetreiveMessageErrorDecoder decoder;
    private Request request;

    @BeforeEach
    void setUp() {
        decoder = new RetreiveMessageErrorDecoder();
        request = Request.create(Request.HttpMethod.GET, "/api/test", Collections.emptyMap(), null, StandardCharsets.UTF_8, null);
    }

    @Test
    void testDecode_Status400_ReturnsBusinessException() {
        Response response = Response.builder()
                .status(400)
                .reason("Bad Request")
                .request(request)
                .body("Custom 400 error message", StandardCharsets.UTF_8)
                .build();

        Exception ex = decoder.decode("testMethod", response);

        assertInstanceOf(BusinessException.class, ex);
        assertEquals("Custom 400 error message", ex.getMessage());
    }

    @Test
    void testDecode_Status404_ReturnsResourceNotFoundException() {
        Response response = Response.builder()
                .status(404)
                .reason("Not Found")
                .request(request)
                .body("Product not found", StandardCharsets.UTF_8)
                .build();

        Exception ex = decoder.decode("testMethod", response);

        assertInstanceOf(ResourceNotFoundException.class, ex);
        assertEquals("Product not found", ex.getMessage());
    }

    @Test
    void testDecode_EmptyBody_ReturnsDefaultMessage() {
        Response response = Response.builder()
                .status(400)
                .reason("Bad Request")
                .request(request)
                .body("", StandardCharsets.UTF_8)
                .build();

        Exception ex = decoder.decode("testMethod", response);

        assertInstanceOf(BusinessException.class, ex);
        assertEquals("Unknown error from external service", ex.getMessage());
    }

    @Test
    void testDecode_NullInputStream_ReturnsDefaultMessage() throws IOException {
        Response.Body mockBody = mock(Response.Body.class);
        when(mockBody.asInputStream()).thenReturn(null);

        Response response = Response.builder()
                .status(400)
                .reason("Bad Request")
                .request(request)
                .body(mockBody)
                .build();

        Exception ex = decoder.decode("testMethod", response);

        assertInstanceOf(BusinessException.class, ex);
        assertEquals("Unknown error from external service", ex.getMessage());
    }

    @Test
    void testDecode_IOExceptionReadingBody_ReturnsFallbackMessage() throws IOException {
        Response.Body mockBody = mock(Response.Body.class);
        InputStream mockStream = mock(InputStream.class);
        when(mockBody.asInputStream()).thenReturn(mockStream);
        when(mockStream.read(any(byte[].class))).thenThrow(new IOException("Simulated read error"));

        Response response = Response.builder()
                .status(400)
                .reason("Bad Request")
                .request(request)
                .body(mockBody)
                .build();

        Exception ex = decoder.decode("testMethod", response);

        assertInstanceOf(BusinessException.class, ex);
        assertEquals("Failed to process error response", ex.getMessage());
    }

    @Test
    void testDecode_OtherStatus_DefaultErrorDecoder() {
        Response response = Response.builder()
                .status(500)
                .reason("Internal Server Error")
                .request(request)
                .body("Internal crash", StandardCharsets.UTF_8)
                .build();

        Exception ex = decoder.decode("testMethod", response);

        assertNotNull(ex);
        assertFalse(ex instanceof BusinessException);
        assertFalse(ex instanceof ResourceNotFoundException);
    }
}
