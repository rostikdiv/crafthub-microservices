package com.crafthub.cart_service.config;

import feign.RequestInterceptor;
import feign.codec.ErrorDecoder;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

/**
 * Configuration for Feign clients in the Cart Service.
 * Includes error decoding and security header propagation.
 */
@Configuration
public class FeignConfig {

    @Bean
    public ErrorDecoder errorDecoder() {
        return new RetreiveMessageErrorDecoder();
    }

    /**
     * Propagates security headers (user session, permissions) from the incoming
     * request
     * to outgoing Feign calls.
     */
    @Bean
    public RequestInterceptor requestInterceptor() {
        return requestTemplate -> {
            ServletRequestAttributes attributes = (ServletRequestAttributes) RequestContextHolder
                    .getRequestAttributes();
            if (attributes != null) {
                HttpServletRequest request = attributes.getRequest();

                // User Identity
                String userId = request.getHeader("X-User-Id");
                if (userId != null)
                    requestTemplate.header("X-User-Id", userId);

                // Permissions Context
                String userPermissions = request.getHeader("X-User-Permissions");
                if (userPermissions != null)
                    requestTemplate.header("X-User-Permissions", userPermissions);

                // Email Context
                String userEmail = request.getHeader("X-User-Email");
                if (userEmail != null)
                    requestTemplate.header("X-User-Email", userEmail);

                // Role Context
                String userRole = request.getHeader("X-User-Role");
                if (userRole != null)
                    requestTemplate.header("X-User-Role", userRole);
            }

        };
    }
}