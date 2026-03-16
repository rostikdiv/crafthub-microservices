package com.crafthub.delivery_service.config;

import feign.RequestInterceptor;
import feign.codec.ErrorDecoder;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

/**
 * Configuration for Feign clients, including error decoding and request
 * interceptors
 * for propagating security headers across microservices.
 */
@Configuration
public class FeignConfig {

    @Bean
    public ErrorDecoder errorDecoder() {
        return new RetreiveMessageErrorDecoder();
    }

    @Bean
    public RequestInterceptor requestInterceptor() {
        return requestTemplate -> {
            ServletRequestAttributes attributes = (ServletRequestAttributes) RequestContextHolder
                    .getRequestAttributes();

            if (attributes != null) {
                HttpServletRequest request = attributes.getRequest();

                // List of all security headers propagated to downstream services

                // 1. User ID (Critical for authorization)
                String userId = request.getHeader("X-User-Id");
                if (userId != null) {
                    requestTemplate.header("X-User-Id", userId);
                }

                // 2. Email (Used for logging or notifications)
                String userEmail = request.getHeader("X-User-Email");
                if (userEmail != null) {
                    requestTemplate.header("X-User-Email", userEmail);
                }

                // 3. User Role (Used for basic authorization)
                String userRole = request.getHeader("X-User-Role");
                if (userRole != null) {
                    requestTemplate.header("X-User-Role", userRole);
                }

                // 4. Permissions (Used for fine-grained @PreAuthorize checks)
                String userPermissions = request.getHeader("X-User-Permissions");
                if (userPermissions != null) {
                    requestTemplate.header("X-User-Permissions", userPermissions);
                }

                // 5. Verification Status (Used for business logic constraints)
                String isVerified = request.getHeader("X-User-Is-Verified");
                if (isVerified != null) {
                    requestTemplate.header("X-User-Is-Verified", isVerified);
                }
            }
        };
    }
}