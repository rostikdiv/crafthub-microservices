package com.crafthub.user_service.config.filter;

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

public class HeaderAuthenticationFilter extends OncePerRequestFilter {

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain chain)
            throws ServletException, IOException {

        // 1. Читаємо заголовки
        String userId = request.getHeader("X-User-Id");
        String permissionsHeader = request.getHeader("X-User-Permissions");
        String path = request.getRequestURI();

        // 🔥 ЛОГИ ДЛЯ ДІАГНОСТИКИ
        System.out.println("========================================");
        System.out.println("📥 [User Service] Filter hit: " + path);
        System.out.println("🔎 [User Service] X-User-Id: " + userId);
        System.out.println("🔎 [User Service] X-User-Permissions: " + permissionsHeader);

        if (userId != null && SecurityContextHolder.getContext().getAuthentication() == null) {
            List<SimpleGrantedAuthority> authorities = Collections.emptyList();

            if (permissionsHeader != null && !permissionsHeader.isEmpty()) {
                authorities = Arrays.stream(permissionsHeader.split(","))
                        .map(SimpleGrantedAuthority::new)
                        .collect(Collectors.toList());
            }

            System.out.println("✅ [User Service] Authenticating user " + userId + " with authorities: " + authorities);

            // Створюємо принципала
            UsernamePasswordAuthenticationToken auth = new UsernamePasswordAuthenticationToken(
                    userId,
                    null,
                    authorities
            );

            SecurityContextHolder.getContext().setAuthentication(auth);
        } else {
            System.out.println("⚠️ [User Service] Authentication skipped (header missing or already auth)");
        }
        System.out.println("========================================");

        chain.doFilter(request, response);
    }
}