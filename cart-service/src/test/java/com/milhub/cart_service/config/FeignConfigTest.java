package com.milhub.cart_service.config;

import feign.RequestInterceptor;
import feign.RequestTemplate;
import feign.codec.ErrorDecoder;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

import java.util.Collection;

import static org.junit.jupiter.api.Assertions.*;

@DisplayName("FeignConfig Unit Tests")
class FeignConfigTest {

    private FeignConfig feignConfig;

    @BeforeEach
    void setUp() {
        feignConfig = new FeignConfig();
    }

    @AfterEach
    void tearDown() {
        RequestContextHolder.resetRequestAttributes();
    }

    @Test
    void testErrorDecoderBean() {
        ErrorDecoder decoder = feignConfig.errorDecoder();
        assertNotNull(decoder);
        assertInstanceOf(RetreiveMessageErrorDecoder.class, decoder);
    }

    @Test
    void testRequestInterceptor_AllHeadersPresent() {
        RequestInterceptor interceptor = feignConfig.requestInterceptor();

        MockHttpServletRequest request = new MockHttpServletRequest();
        request.addHeader("X-User-Id", "uuid-123");
        request.addHeader("X-User-Permissions", "READ,WRITE");
        request.addHeader("X-User-Email", "user@milhub.ua");
        request.addHeader("X-User-Role", "ADMIN");
        RequestContextHolder.setRequestAttributes(new ServletRequestAttributes(request));

        RequestTemplate template = new RequestTemplate();
        interceptor.apply(template);

        assertTrue(template.headers().containsKey("X-User-Id"));
        assertTrue(template.headers().get("X-User-Id").contains("uuid-123"));
        assertTrue(template.headers().containsKey("X-User-Permissions"));
        assertTrue(template.headers().get("X-User-Permissions").contains("READ,WRITE"));
        assertTrue(template.headers().containsKey("X-User-Email"));
        assertTrue(template.headers().get("X-User-Email").contains("user@milhub.ua"));
        assertTrue(template.headers().containsKey("X-User-Role"));
        assertTrue(template.headers().get("X-User-Role").contains("ADMIN"));
    }

    @Test
    void testRequestInterceptor_NoHeadersPresent() {
        RequestInterceptor interceptor = feignConfig.requestInterceptor();

        MockHttpServletRequest request = new MockHttpServletRequest();
        RequestContextHolder.setRequestAttributes(new ServletRequestAttributes(request));

        RequestTemplate template = new RequestTemplate();
        interceptor.apply(template);

        assertFalse(template.headers().containsKey("X-User-Id"));
        assertFalse(template.headers().containsKey("X-User-Permissions"));
        assertFalse(template.headers().containsKey("X-User-Email"));
        assertFalse(template.headers().containsKey("X-User-Role"));
    }

    @Test
    void testRequestInterceptor_NullRequestContext() {
        RequestInterceptor interceptor = feignConfig.requestInterceptor();
        RequestContextHolder.resetRequestAttributes();

        RequestTemplate template = new RequestTemplate();
        assertDoesNotThrow(() -> interceptor.apply(template));
        assertTrue(template.headers().isEmpty());
    }
}
