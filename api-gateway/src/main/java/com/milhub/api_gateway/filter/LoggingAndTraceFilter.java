package com.milhub.api_gateway.filter;

import lombok.extern.slf4j.Slf4j;
import org.springframework.cloud.gateway.filter.GatewayFilterChain;
import org.springframework.cloud.gateway.filter.GlobalFilter;
import org.springframework.core.Ordered;
import org.springframework.http.server.reactive.ServerHttpRequest;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Mono;

import java.util.UUID;

@Component
@Slf4j
public class LoggingAndTraceFilter implements GlobalFilter, Ordered {

    private static final String CORRELATION_ID_HEADER_NAME = "X-Correlation-Id";

    @Override
    public Mono<Void> filter(ServerWebExchange exchange, GatewayFilterChain chain) {
        long startTime = System.currentTimeMillis();

        ServerHttpRequest request = exchange.getRequest();
        String correlationId = request.getHeaders().getFirst(CORRELATION_ID_HEADER_NAME);

        // Generate Correlation ID if missing
        if (correlationId == null || correlationId.isBlank()) {
            correlationId = UUID.randomUUID().toString();
            request = request.mutate()
                    .header(CORRELATION_ID_HEADER_NAME, correlationId)
                    .build();
        }

        final String finalCorrelationId = correlationId;
        final String method = request.getMethod().name();
        final String path = request.getURI().getPath();

        log.info("[CorrelationId: {}] Start Request: {} {}", finalCorrelationId, method, path);

        // Continue chain with the mutated request
        return chain.filter(exchange.mutate().request(request).build())
                .then(Mono.fromRunnable(() -> {
                    long duration = System.currentTimeMillis() - startTime;
                    int statusCode = -1;
                    if (exchange.getResponse().getStatusCode() != null) {
                        statusCode = exchange.getResponse().getStatusCode().value();
                    }
                    log.info("[CorrelationId: {}] End Request: {} {} - Status: {} - Duration: {}ms",
                            finalCorrelationId, method, path, statusCode, duration);
                }));
    }

    @Override
    public int getOrder() {
        return Ordered.HIGHEST_PRECEDENCE;
    }
}
