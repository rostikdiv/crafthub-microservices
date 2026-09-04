package com.milhub.user_service.config;

import com.milhub.user_service.exception.BusinessException;
import com.milhub.user_service.exception.ResourceNotFoundException;
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
                "/api/users/123",
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
                .body("Invalid user payload", StandardCharsets.UTF_8)
                .build();

        Exception ex = decoder.decode("UserServiceClient#getUser", response);

        assertThat(ex).isInstanceOf(BusinessException.class);
        assertThat(ex.getMessage()).isEqualTo("Invalid user payload");
    }

    @Test
    void decode_WhenStatus404_ShouldReturnResourceNotFoundException() {
        Response response = Response.builder()
                .status(404)
                .reason("Not Found")
                .request(dummyRequest)
                .headers(Collections.emptyMap())
                .body("User not found", StandardCharsets.UTF_8)
                .build();

        Exception ex = decoder.decode("UserServiceClient#getUser", response);

        assertThat(ex).isInstanceOf(ResourceNotFoundException.class);
        assertThat(ex.getMessage()).isEqualTo("User not found");
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

        Exception ex = decoder.decode("UserServiceClient#getUser", response);

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
                .body("Boom", StandardCharsets.UTF_8)
                .build();

        Exception ex = decoder.decode("UserServiceClient#getUser", response);

        assertThat(ex).isInstanceOf(feign.FeignException.class);
    }
}
