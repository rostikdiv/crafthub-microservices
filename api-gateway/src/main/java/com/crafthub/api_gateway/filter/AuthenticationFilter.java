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

        log.debug("Processing request to path: {}", path);

        // 1. Перевіряємо, чи є шлях публічним
        if (isPublicEndpoint(path)) {
            log.info("Public endpoint: {} - skipping auth.", path);
            return chain.filter(exchange);
        }

        // 2. Отримуємо заголовок Authorization
        String authHeader = request.getHeaders().getFirst(HttpHeaders.AUTHORIZATION);

        // 3. Перевіряємо заголовок
        if (authHeader == null || !authHeader.startsWith("Bearer ")) {
            log.warn("Missing or invalid Authorization Header for path: {}", path);
            return unauthorizedResponse(exchange, "Missing or invalid Authorization Header");
        }

        // 4. Витягуємо сам токен
        String token = authHeader.substring(7);
        log.debug("Extracted token: {}...", token.substring(0, Math.min(20, token.length())));

        // 5. Валідуємо токен
        try {
            if (!jwtUtil.isTokenValid(token)) {
                log.warn("Invalid JWT token for path: {}", path);
                return unauthorizedResponse(exchange, "Invalid JWT token");
            }

            // Додатково логуємо успішну валідацію
            log.info("✓ Token validated successfully for path: {}", path);

        } catch (Exception e) {
            log.error("JWT validation error for path: {}: {}", path, e.getMessage());
            return unauthorizedResponse(exchange, "JWT validation error: " + e.getMessage());
        }

        // 6. Токен валідний - пропускаємо запит далі
        log.info("Valid token. Forwarding request to: {}", path);
        return chain.filter(exchange);
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