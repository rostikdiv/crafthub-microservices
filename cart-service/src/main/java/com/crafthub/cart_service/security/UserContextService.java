package com.crafthub.cart_service.security;

import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

import java.util.UUID;

@Service
@RequiredArgsConstructor
public class UserContextService {

    // We avoid directly injecting HttpServletRequest as a field
    // to prevent threading issues, retrieving it from the RequestContextHolder
    // instead.

    public UUID getUserId() {
        String userId = getHeader("X-User-Id");
        if (userId == null)
            throw new RuntimeException("User ID header missing");
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
        return Boolean.parseBoolean(isVerified); // returns false if null
    }

    private String getHeader(String headerName) {
        ServletRequestAttributes attributes = (ServletRequestAttributes) RequestContextHolder.getRequestAttributes();
        if (attributes == null) {
            // This can happen if the method is called outside of an HTTP request (e.g., in
            // a Kafka Listener)
            return null;
        }
        HttpServletRequest request = attributes.getRequest();
        return request.getHeader(headerName);
    }
}