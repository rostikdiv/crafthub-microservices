package com.milhub.product_service.config;

import com.milhub.product_service.exception.BusinessException;
import com.milhub.product_service.exception.ResourceNotFoundException;
import feign.Request;
import feign.RequestTemplate;
import feign.Response;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.nio.charset.StandardCharsets;
import java.util.Collections;

import static org.assertj.core.api.Assertions.assertThat;

class RetreiveMessageErrorDecoderTest {

    private RetreiveMessageErrorDecoder decoder;

    @BeforeEach
    void setUp() {
        decoder = new RetreiveMessageErrorDecoder();
    }

    private Response createResponse(int status, String body) {
        Request request = Request.create(
                Request.HttpMethod.GET,
                "/api/test",
                Collections.emptyMap(),
                null,
                new RequestTemplate()
        );
        return Response.builder()
                .status(status)
                .reason("Reason")
                .request(request)
                .body(body != null ? body.getBytes(StandardCharsets.UTF_8) : null)
                .build();
    }

    @Test
    @DisplayName("decode: translates 400 Bad Request to BusinessException with response body")
    void decode_WhenStatus400_ShouldReturnBusinessException() {
        Response response = createResponse(400, "Insufficient stock available");

        Exception exception = decoder.decode("ProductClient#reduceStock", response);

        assertThat(exception).isInstanceOf(BusinessException.class);
        assertThat(exception.getMessage()).isEqualTo("Insufficient stock available");
    }

    @Test
    @DisplayName("decode: translates 404 Not Found to ResourceNotFoundException")
    void decode_WhenStatus404_ShouldReturnResourceNotFoundException() {
        Response response = createResponse(404, "Product not found");

        Exception exception = decoder.decode("ProductClient#getProduct", response);

        assertThat(exception).isInstanceOf(ResourceNotFoundException.class);
        assertThat(exception.getMessage()).isEqualTo("Product not found");
    }

    @Test
    @DisplayName("decode: handles empty body with fallback message")
    void decode_WhenBodyIsNull_ShouldUseFallbackMessage() {
        Response response = createResponse(400, null);

        Exception exception = decoder.decode("ProductClient#reduceStock", response);

        assertThat(exception).isInstanceOf(BusinessException.class);
        assertThat(exception.getMessage()).isEqualTo("Unknown error from external service");
    }

    @Test
    @DisplayName("decode: uses default decoder for status 500")
    void decode_WhenStatus500_ShouldReturnFeignException() {
        Response response = createResponse(500, "Internal Server Error");

        Exception exception = decoder.decode("ProductClient#test", response);

        assertThat(exception).isNotNull();
        assertThat(exception).isNotInstanceOf(BusinessException.class);
        assertThat(exception).isNotInstanceOf(ResourceNotFoundException.class);
    }
}
