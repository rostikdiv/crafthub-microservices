package com.crafthub.delivery_service.config;

import feign.RequestInterceptor;
import feign.codec.ErrorDecoder;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

@Configuration
public class FeignConfig {


    @Bean
    public ErrorDecoder errorDecoder() {
        return new RetreiveMessageErrorDecoder();
    }

    @Bean
    public RequestInterceptor requestInterceptor() {
        return requestTemplate -> {
            ServletRequestAttributes attributes = (ServletRequestAttributes) RequestContextHolder.getRequestAttributes();

            if (attributes != null) {
                HttpServletRequest request = attributes.getRequest();

                // === Список усіх заголовків безпеки, які ми прокидаємо ===

                // 1. ID користувача (Критично важливо)
                String userId = request.getHeader("X-User-Id");
                if (userId != null) {
                    requestTemplate.header("X-User-Id", userId);
                }

                // 2. Email (Для логування або повідомлень)
                String userEmail = request.getHeader("X-User-Email");
                if (userEmail != null) {
                    requestTemplate.header("X-User-Email", userEmail);
                }

                // 3. Роль (Для грубої авторизації)
                String userRole = request.getHeader("X-User-Role");
                if (userRole != null) {
                    requestTemplate.header("X-User-Role", userRole);
                }

                // 4. Права доступу (Для точкової авторизації @PreAuthorize)
                String userPermissions = request.getHeader("X-User-Permissions");
                if (userPermissions != null) {
                    requestTemplate.header("X-User-Permissions", userPermissions);
                }

                // 5. Статус верифікації (Для бізнес-логіки)
                String isVerified = request.getHeader("X-User-Is-Verified");
                if (isVerified != null) {
                    requestTemplate.header("X-User-Is-Verified", isVerified);
                }
            }
        };
    }
}