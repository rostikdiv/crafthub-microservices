package com.crafthub.product_service.config;

import feign.RequestInterceptor;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

@Configuration
public class FeignConfig {

    @Bean
    public RequestInterceptor requestInterceptor() {
        return requestTemplate -> {
            ServletRequestAttributes attributes = (ServletRequestAttributes) RequestContextHolder.getRequestAttributes();
            if (attributes != null) {
                HttpServletRequest request = attributes.getRequest();

                String userId = request.getHeader("X-User-Id");
                if (userId != null) requestTemplate.header("X-User-Id", userId);

                String userPermissions = request.getHeader("X-User-Permissions");
                if (userPermissions != null) requestTemplate.header("X-User-Permissions", userPermissions);

                // Інші корисні заголовки
                String userEmail = request.getHeader("X-User-Email");
                if (userEmail != null) requestTemplate.header("X-User-Email", userEmail);

                String userRole = request.getHeader("X-User-Role");
                if (userRole != null) requestTemplate.header("X-User-Role", userRole);
            }
        };
    }
}