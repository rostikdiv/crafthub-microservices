package com.crafthub.product_service.config.security;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

/**
 * Custom security filter that extracts user identity and permissions from HTTP
 * headers.
 * These headers are expected to be set by the API Gateway.
 */
public class HeaderAuthenticationFilter extends OncePerRequestFilter {

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain chain)
            throws ServletException, IOException {

        String userId = request.getHeader("X-User-Id");
        String permissionsHeader = request.getHeader("X-User-Permissions");

        // For debugging (can be removed later)
        if (userId != null) {
            System.out.println("🛡️ [ProductService] User: " + userId);
            System.out.println("🛡️ [ProductService] Raw Permissions: " + permissionsHeader);
        }

        if (userId != null && SecurityContextHolder.getContext().getAuthentication() == null) {

            List<SimpleGrantedAuthority> authorities = Collections.emptyList();

            if (permissionsHeader != null && !permissionsHeader.isEmpty()) {
                authorities = Arrays.stream(permissionsHeader.split(","))
                        .map(String::trim) // Trim whitespace to handle multiple permissions
                        .filter(s -> !s.isEmpty()) // Skip empty segments if any
                        .map(SimpleGrantedAuthority::new)
                        .collect(Collectors.toList());
            }

            System.out.println("✅ [ProductService] Authorities: " + authorities);

            UsernamePasswordAuthenticationToken auth = new UsernamePasswordAuthenticationToken(
                    userId,
                    null,
                    authorities);

            SecurityContextHolder.getContext().setAuthentication(auth);
        }

        chain.doFilter(request, response);
    }
}