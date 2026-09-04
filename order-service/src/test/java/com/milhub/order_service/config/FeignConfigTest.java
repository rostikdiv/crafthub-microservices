package com.milhub.order_service.config;

import feign.RequestInterceptor;
import feign.RequestTemplate;
import jakarta.servlet.http.HttpServletRequest;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

import java.util.Collection;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class FeignConfigTest {

    @Mock
    private HttpServletRequest request;

    private final FeignConfig feignConfig = new FeignConfig();

    @BeforeEach
    void setUp() {
        RequestContextHolder.setRequestAttributes(new ServletRequestAttributes(request));
    }

    @AfterEach
    void tearDown() {
        RequestContextHolder.resetRequestAttributes();
    }

    @Test
    @DisplayName("errorDecoder bean is created")
    void testErrorDecoderBean() {
        assertNotNull(feignConfig.errorDecoder());
    }

    @Test
    @DisplayName("requestInterceptor propagates all 5 headers when present")
    void testRequestInterceptor_WithHeaders() {
        when(request.getHeader("X-User-Id")).thenReturn("user-1");
        when(request.getHeader("X-User-Email")).thenReturn("user@milhub.com");
        when(request.getHeader("X-User-Role")).thenReturn("BUYER");
        when(request.getHeader("X-User-Permissions")).thenReturn("order:create");
        when(request.getHeader("X-User-Is-Verified")).thenReturn("true");

        RequestInterceptor interceptor = feignConfig.requestInterceptor();
        RequestTemplate template = new RequestTemplate();

        interceptor.apply(template);

        Map<String, Collection<String>> headers = template.headers();
        assertTrue(headers.get("X-User-Id").contains("user-1"));
        assertTrue(headers.get("X-User-Email").contains("user@milhub.com"));
        assertTrue(headers.get("X-User-Role").contains("BUYER"));
        assertTrue(headers.get("X-User-Permissions").contains("order:create"));
        assertTrue(headers.get("X-User-Is-Verified").contains("true"));
    }

    @Test
    @DisplayName("requestInterceptor skips null headers")
    void testRequestInterceptor_NullHeaders() {
        when(request.getHeader(anyString())).thenReturn(null);

        RequestInterceptor interceptor = feignConfig.requestInterceptor();
        RequestTemplate template = new RequestTemplate();

        interceptor.apply(template);

        assertTrue(template.headers().isEmpty());
    }

    @Test
    @DisplayName("requestInterceptor handles null RequestAttributes gracefully")
    void testRequestInterceptor_NullAttributes() {
        RequestContextHolder.resetRequestAttributes();

        RequestInterceptor interceptor = feignConfig.requestInterceptor();
        RequestTemplate template = new RequestTemplate();

        assertDoesNotThrow(() -> interceptor.apply(template));
        assertTrue(template.headers().isEmpty());
    }
}
