package com.crafthub.user_service.config;

import feign.RequestInterceptor;
import feign.codec.ErrorDecoder;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

/**
 * Configuration for Feign clients, including error decoding and security header
 * propagation.
 */
@Configuration
public class FeignConfig {

    /**
     * Custom error decoder for processing Feign exceptions from external
     * microservices.
     */
    @Bean
    public ErrorDecoder errorDecoder() {
        return new RetreiveMessageErrorDecoder();
    }

    /**
     * Interceptor to propagate security headers (User ID, Email, Roles,
     * Permissions)
     * from the current request to outgoing Feign calls.
     */
    @Bean
    public RequestInterceptor requestInterceptor() {
        return requestTemplate -> {
            ServletRequestAttributes attributes = (ServletRequestAttributes) RequestContextHolder
                    .getRequestAttributes();

            if (attributes != null) {
                HttpServletRequest request = attributes.getRequest();

                // List of security headers to be propagated
                String userId = request.getHeader("X-User-Id");
                if (userId != null) {
                    requestTemplate.header("X-User-Id", userId);
                }

                String userEmail = request.getHeader("X-User-Email");
                if (userEmail != null) {
                    requestTemplate.header("X-User-Email", userEmail);
                }

                String userRole = request.getHeader("X-User-Role");
                if (userRole != null) {
                    requestTemplate.header("X-User-Role", userRole);
                }

                String userPermissions = request.getHeader("X-User-Permissions");
                if (userPermissions != null) {
                    requestTemplate.header("X-User-Permissions", userPermissions);
                }

                String isVerified = request.getHeader("X-User-Is-Verified");
                if (isVerified != null) {
                    requestTemplate.header("X-User-Is-Verified", isVerified);
                }
            }
        };
    }
}