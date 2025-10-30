package com.crafthub.api_gateway.filter;

import com.crafthub.api_gateway.util.JwtUtil;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cloud.gateway.filter.GatewayFilterChain;
import org.springframework.cloud.gateway.filter.GlobalFilter;
import org.springframework.core.Ordered;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.server.reactive.ServerHttpRequest;
import org.springframework.http.server.reactive.ServerHttpResponse;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Mono;

import java.util.List;

@Component
@RequiredArgsConstructor
@Slf4j
public class AuthenticationFilter implements GlobalFilter, Ordered {

    private final JwtUtil jwtUtil;

    // Список "публічних" ендпоінтів, які не потребують токена
    private final List<String> publicEndpoints = List.of(
            "/api/v1/auth/register",
            "/api/v1/auth/login"
    );

    @Override
    public Mono<Void> filter(ServerWebExchange exchange, GatewayFilterChain chain) {
        ServerHttpRequest request = exchange.getRequest();
        String path = request.getURI().getPath();

        if (isPublicEndpoint(path)) {
            log.info("Public endpoint: {} - skipping auth.", path);
            return chain.filter(exchange);
        }

        String authHeader = request.getHeaders().getFirst(HttpHeaders.AUTHORIZATION);

        if (authHeader == null || !authHeader.startsWith("Bearer ")) {
            log.warn("Missing or invalid Authorization Header for path: {}", path);
            return unauthorizedResponse(exchange, "Missing or invalid Authorization Header");
        }

        String token = authHeader.substring(7);

        try {
            if (!jwtUtil.isTokenValid(token)) {
                log.warn("Invalid JWT token for path: {}", path);
                return unauthorizedResponse(exchange, "Invalid JWT token");
            }

            // 1. Витягуємо email з токена
            String userEmail = jwtUtil.extractUsername(token);
            log.debug("User Email from token: {}", userEmail);

            // 2. Додаємо email в заголовок для downstream-сервісів
            ServerHttpRequest modifiedRequest = exchange.getRequest().mutate()
                    .header("X-User-Email", userEmail)
                    .build();

            // 3. Передаємо *модифікований* запит
            return chain.filter(exchange.mutate().request(modifiedRequest).build());


        } catch (Exception e) {
            log.error("JWT validation error for path: {}: {}", path, e.getMessage());
            return unauthorizedResponse(exchange, "JWT validation error: " + e.getMessage());
        }
    }

    private boolean isPublicEndpoint(String path) {
        boolean isPublic = publicEndpoints.stream()
                .anyMatch(publicPath -> path.equals(publicPath) || path.startsWith(publicPath + "/"));
        log.debug("Path {} is public: {}", path, isPublic);
        return isPublic;
    }

    private Mono<Void> unauthorizedResponse(ServerWebExchange exchange, String message) {
        ServerHttpResponse response = exchange.getResponse();
        response.setStatusCode(HttpStatus.UNAUTHORIZED);
        response.getHeaders().add("Content-Type", "application/json");

        String body = String.format("{\"error\": \"Unauthorized\", \"message\": \"%s\"}", message);
        return response.writeWith(Mono.just(response.bufferFactory().wrap(body.getBytes())));
    }

    @Override
    public int getOrder() {
        return -1;
    }
}