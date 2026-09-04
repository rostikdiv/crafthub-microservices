package com.milhub.api_gateway.config;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.cloud.gateway.filter.ratelimit.KeyResolver;
import org.springframework.mock.http.server.reactive.MockServerHttpRequest;
import org.springframework.mock.web.server.MockServerWebExchange;
import org.springframework.web.server.ServerWebExchange;

import java.net.InetSocketAddress;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.*;

@DisplayName("RateLimiterConfig Tests")
class RateLimiterConfigTest {

    private RateLimiterConfig config;
    private KeyResolver keyResolver;

    @BeforeEach
    void setUp() {
        config = new RateLimiterConfig();
        keyResolver = config.ipKeyResolver();
    }

    @Test
    void testResolve_WithRemoteAddress() {
        MockServerHttpRequest request = MockServerHttpRequest.get("/test")
                .remoteAddress(new InetSocketAddress("192.168.1.50", 12345))
                .build();
        MockServerWebExchange exchange = MockServerWebExchange.from(request);

        String ip = keyResolver.resolve(exchange).block();
        assertEquals("192.168.1.50", ip);
    }

    @Test
    void testResolve_NullRemoteAddress_ReturnsAnonymous() {
        // By default MockServerHttpRequest has null remote address
        MockServerHttpRequest request = MockServerHttpRequest.get("/test").build();
        MockServerWebExchange exchange = MockServerWebExchange.from(request);

        String ip = keyResolver.resolve(exchange).block();
        assertEquals("anonymous", ip);
    }

    @Test
    void testResolve_ExceptionThrown_ReturnsAnonymous() {
        ServerWebExchange exchange = mock(ServerWebExchange.class);
        when(exchange.getRequest()).thenThrow(new RuntimeException("Network failure"));

        String ip = keyResolver.resolve(exchange).block();
        assertEquals("anonymous", ip);
    }
}
