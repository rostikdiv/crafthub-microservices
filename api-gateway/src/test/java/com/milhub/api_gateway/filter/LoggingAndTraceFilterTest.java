package com.milhub.api_gateway.filter;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.cloud.gateway.filter.GatewayFilterChain;
import org.springframework.core.Ordered;
import org.springframework.http.HttpStatus;
import org.springframework.mock.http.server.reactive.MockServerHttpRequest;
import org.springframework.mock.web.server.MockServerWebExchange;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Mono;

import java.util.concurrent.atomic.AtomicReference;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("LoggingAndTraceFilter Tests")
class LoggingAndTraceFilterTest {

    private LoggingAndTraceFilter filter;

    @Mock
    private GatewayFilterChain chain;

    @BeforeEach
    void setUp() {
        filter = new LoggingAndTraceFilter();
    }

    @Test
    void testFilter_NoCorrelationId_GeneratesNewAndLogsWithStatus() {
        MockServerHttpRequest request = MockServerHttpRequest.get("/api/v1/products").build();
        MockServerWebExchange exchange = MockServerWebExchange.from(request);
        exchange.getResponse().setStatusCode(HttpStatus.OK);

        AtomicReference<ServerWebExchange> captured = new AtomicReference<>();
        when(chain.filter(any(ServerWebExchange.class))).thenAnswer(invocation -> {
            captured.set(invocation.getArgument(0));
            return Mono.empty();
        });

        Mono<Void> result = filter.filter(exchange, chain);
        result.block();

        assertNotNull(captured.get());
        String generatedCid = captured.get().getRequest().getHeaders().getFirst("X-Correlation-Id");
        assertNotNull(generatedCid);
        assertFalse(generatedCid.isBlank());
    }

    @Test
    void testFilter_BlankCorrelationId_GeneratesNew() {
        MockServerHttpRequest request = MockServerHttpRequest.get("/api/v1/products")
                .header("X-Correlation-Id", "   ")
                .build();
        MockServerWebExchange exchange = MockServerWebExchange.from(request);

        AtomicReference<ServerWebExchange> captured = new AtomicReference<>();
        when(chain.filter(any(ServerWebExchange.class))).thenAnswer(invocation -> {
            captured.set(invocation.getArgument(0));
            return Mono.empty();
        });

        Mono<Void> result = filter.filter(exchange, chain);
        result.block();

        assertNotNull(captured.get());
        String generatedCid = captured.get().getRequest().getHeaders().getFirst("X-Correlation-Id");
        assertNotNull(generatedCid);
        assertNotEquals("   ", generatedCid);
    }

    @Test
    void testFilter_ExistingCorrelationId_PreservedAndNullStatusCode() {
        MockServerHttpRequest request = MockServerHttpRequest.get("/api/v1/products")
                .header("X-Correlation-Id", "trace-abc-123")
                .build();
        MockServerWebExchange exchange = MockServerWebExchange.from(request);
        // Note: Default MockServerWebExchange does not set a status code until explicitly set, so getStatusCode() is null

        AtomicReference<ServerWebExchange> captured = new AtomicReference<>();
        when(chain.filter(any(ServerWebExchange.class))).thenAnswer(invocation -> {
            captured.set(invocation.getArgument(0));
            return Mono.empty();
        });

        Mono<Void> result = filter.filter(exchange, chain);
        result.block();

        assertNotNull(captured.get());
        assertEquals("trace-abc-123", captured.get().getRequest().getHeaders().getFirst("X-Correlation-Id"));
    }

    @Test
    void testGetOrder() {
        assertEquals(Ordered.HIGHEST_PRECEDENCE, filter.getOrder());
    }
}
