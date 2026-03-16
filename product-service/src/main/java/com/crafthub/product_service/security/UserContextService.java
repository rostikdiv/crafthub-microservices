package com.crafthub.product_service.security;

import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

import java.util.UUID;

/**
 * Service for accessing user context information from the current HTTP request.
 * Useful for retrieving user ID, email, role, and verification status
 * propagated by the API Gateway.
 */
@Service
@RequiredArgsConstructor
public class UserContextService {

    // We do not inject HttpServletRequest directly into a field to avoid
    // thread-safety issues; instead, we retrieve it from the request context.

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
            // This can happen if the method is called outside of an HTTP request (e.g.,
            // Kafka Listener)
            return null;
        }
        HttpServletRequest request = attributes.getRequest();
        return request.getHeader(headerName);
    }
}