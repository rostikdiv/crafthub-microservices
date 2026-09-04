package com.milhub.api_gateway.config;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.mock.http.server.reactive.MockServerHttpRequest;
import org.springframework.mock.web.server.MockServerWebExchange;
import org.springframework.security.config.web.server.ServerHttpSecurity;
import org.springframework.security.web.server.SecurityWebFilterChain;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.reactive.CorsConfigurationSource;

import static org.junit.jupiter.api.Assertions.*;

@DisplayName("SecurityConfig Tests")
class SecurityConfigTest {

    private SecurityConfig securityConfig;

    @BeforeEach
    void setUp() {
        securityConfig = new SecurityConfig();
        ReflectionTestUtils.setField(securityConfig, "corsAllowedOrigins", "http://localhost:3000, http://custom-domain.com");
    }

    @Test
    void testCorsConfigurationSource() {
        CorsConfigurationSource source = securityConfig.corsConfigurationSource();
        assertNotNull(source);

        MockServerHttpRequest request = MockServerHttpRequest.get("/api/v1/products")
                .header("Origin", "http://localhost:3000")
                .build();
        MockServerWebExchange exchange = MockServerWebExchange.from(request);

        CorsConfiguration config = source.getCorsConfiguration(exchange);
        assertNotNull(config);
        assertTrue(config.getAllowedOrigins().contains("http://localhost:3000"));
        assertTrue(config.getAllowedOrigins().contains("http://custom-domain.com"));
        assertTrue(config.getAllowedOrigins().contains("https://milhub-frontend.vercel.app"));
        assertTrue(config.getAllowedOrigins().contains("http://localhost:5173"));
        assertTrue(config.getAllowCredentials());
        assertEquals(3600L, config.getMaxAge());
        assertTrue(config.getAllowedMethods().contains("GET"));
        assertTrue(config.getAllowedMethods().contains("POST"));
    }

    @Test
    void testSecurityWebFilterChain() {
        ServerHttpSecurity http = ServerHttpSecurity.http();
        SecurityWebFilterChain chain = securityConfig.securityWebFilterChain(http);
        assertNotNull(chain);
    }
}
