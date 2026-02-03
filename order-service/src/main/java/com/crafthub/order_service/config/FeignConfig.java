package com.crafthub.order_service.config;

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

                // Прокидаємо критично важливі заголовки безпеки
                String userId = request.getHeader("X-User-Id");
                String userEmail = request.getHeader("X-User-Email");
                String userRole = request.getHeader("X-User-Role");
                String userPermissions = request.getHeader("X-User-Permissions");
                String isVerified = request.getHeader("X-User-Is-Verified");

                if (userId != null) requestTemplate.header("X-User-Id", userId);
                if (userEmail != null) requestTemplate.header("X-User-Email", userEmail);
                if (userRole != null) requestTemplate.header("X-User-Role", userRole);
                if (userPermissions != null) requestTemplate.header("X-User-Permissions", userPermissions);
                if (isVerified != null) requestTemplate.header("X-User-Is-Verified", isVerified);
            }
        };
    }
}