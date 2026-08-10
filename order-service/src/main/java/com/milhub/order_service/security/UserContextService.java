package com.milhub.order_service.security;


import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

import java.util.UUID;

@Service
@RequiredArgsConstructor
public class UserContextService {

    // Ми не інжектимо HttpServletRequest напряму в поле,
    // щоб уникнути проблем з потоками, а беремо його з контексту.

    public UUID getUserId() {
        String userId = getHeader("X-User-Id");
        if (userId == null) throw new RuntimeException("User ID header missing");
        return UUID.fromString(userId);
    }

    public String getUserEmail() {
        return getHeader("X-User-Email");
    }

    public String getUserRole() {
        return getHeader("X-User-Role");
    }

    public boolean isVerified() {
        String isVerified = getHeader("X-User-Is-Verified");
        return Boolean.parseBoolean(isVerified); // поверне false, якщо null
    }

    private String getHeader(String headerName) {
        ServletRequestAttributes attributes = (ServletRequestAttributes) RequestContextHolder.getRequestAttributes();
        if (attributes == null) {
            // Це може статись, якщо метод викликається не через HTTP запит (наприклад, Kafka Listener)
            return null;
        }
        HttpServletRequest request = attributes.getRequest();
        return request.getHeader(headerName);
    }
}