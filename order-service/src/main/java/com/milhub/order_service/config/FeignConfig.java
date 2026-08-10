package com.milhub.order_service.config;

import feign.RequestInterceptor;
import feign.codec.ErrorDecoder;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

/**
 * Feign client configuration.
 * Includes error handling and security header propagation.
 */
@Configuration
public class FeignConfig {

    @Bean
    public ErrorDecoder errorDecoder() {
        return new RetreiveMessageErrorDecoder();
    }

    /**
     * Interceptor to propagate security headers from the incoming request to
     * outgoing Feign calls.
     */
    @Bean
    public RequestInterceptor requestInterceptor() {
        return requestTemplate -> {
            ServletRequestAttributes attributes = (ServletRequestAttributes) RequestContextHolder
                    .getRequestAttributes();

            if (attributes != null) {
                HttpServletRequest request = attributes.getRequest();

                // Pass security and user context headers

                // 1. User ID (Critical)
                String userId = request.getHeader("X-User-Id");
                if (userId != null) {
                    requestTemplate.header("X-User-Id", userId);
                }

                // 2. Email (For logging or notifications)
                String userEmail = request.getHeader("X-User-Email");
                if (userEmail != null) {
                    requestTemplate.header("X-User-Email", userEmail);
                }

                // 3. User Role
                String userRole = request.getHeader("X-User-Role");
                if (userRole != null) {
                    requestTemplate.header("X-User-Role", userRole);
                }

                // 4. Permissions (For @PreAuthorize)
                String userPermissions = request.getHeader("X-User-Permissions");
                if (userPermissions != null) {
                    requestTemplate.header("X-User-Permissions", userPermissions);
                }

                // 5. Verification status
                String isVerified = request.getHeader("X-User-Is-Verified");
                if (isVerified != null) {
                    requestTemplate.header("X-User-Is-Verified", isVerified);
                }
            }
        };
    }
}