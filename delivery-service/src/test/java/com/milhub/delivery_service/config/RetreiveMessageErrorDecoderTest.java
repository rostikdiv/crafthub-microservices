package com.milhub.delivery_service.config;

import com.milhub.delivery_service.exception.BusinessException;
import com.milhub.delivery_service.exception.ResourceNotFoundException;
import feign.Request;
import feign.Response;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.nio.charset.StandardCharsets;
import java.util.Collections;

import static org.assertj.core.api.Assertions.assertThat;

class RetreiveMessageErrorDecoderTest {

    private RetreiveMessageErrorDecoder decoder;
    private Request dummyRequest;

    @BeforeEach
    void setUp() {
        decoder = new RetreiveMessageErrorDecoder();
        dummyRequest = Request.create(
                Request.HttpMethod.GET,
                "/api/orders/123",
                Collections.emptyMap(),
                null,
                StandardCharsets.UTF_8,
                null
        );
    }

    @Test
    void decode_WhenStatus400_ShouldReturnBusinessException() {
        Response response = Response.builder()
                .status(400)
                .reason("Bad Request")
                .request(dummyRequest)
                .headers(Collections.emptyMap())
                .body("Invalid order parameters", StandardCharsets.UTF_8)
                .build();

        Exception ex = decoder.decode("OrderServiceClient#getOrderById", response);

        assertThat(ex).isInstanceOf(BusinessException.class);
        assertThat(ex.getMessage()).isEqualTo("Invalid order parameters");
    }

    @Test
    void decode_WhenStatus404_ShouldReturnResourceNotFoundException() {
        Response response = Response.builder()
                .status(404)
                .reason("Not Found")
                .request(dummyRequest)
                .headers(Collections.emptyMap())
                .body("Order not found", StandardCharsets.UTF_8)
                .build();

        Exception ex = decoder.decode("OrderServiceClient#getOrderById", response);

        assertThat(ex).isInstanceOf(ResourceNotFoundException.class);
        assertThat(ex.getMessage()).isEqualTo("Order not found");
    }

    @Test
    void decode_WhenBodyIsEmpty_ShouldUseDefaultErrorMessage() {
        Response response = Response.builder()
                .status(400)
                .reason("Bad Request")
                .request(dummyRequest)
                .headers(Collections.emptyMap())
                .body("", StandardCharsets.UTF_8)
                .build();

        Exception ex = decoder.decode("OrderServiceClient#getOrderById", response);

        assertThat(ex).isInstanceOf(BusinessException.class);
        assertThat(ex.getMessage()).isEqualTo("Unknown error from external service");
    }

    @Test
    void decode_WhenOtherStatus_ShouldReturnDefaultFeignException() {
        Response response = Response.builder()
                .status(500)
                .reason("Internal Server Error")
                .request(dummyRequest)
                .headers(Collections.emptyMap())
                .body("Server explosion", StandardCharsets.UTF_8)
                .build();

        Exception ex = decoder.decode("OrderServiceClient#getOrderById", response);

        assertThat(ex).isInstanceOf(feign.FeignException.class);
    }
}
