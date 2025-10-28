package com.crafthub.user_service.config.filter;

import com.crafthub.user_service.service.JwtService;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.lang.NonNull;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.web.authentication.WebAuthenticationDetailsSource;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;

@Component
@RequiredArgsConstructor
@Slf4j
public class JwtAuthenticationFilter extends OncePerRequestFilter {

    private final JwtService jwtService;
    private final UserDetailsService userDetailsService; // ❗️ Наш бін з SecurityConfig

    @Override
    protected void doFilterInternal(
            @NonNull HttpServletRequest request,
            @NonNull HttpServletResponse response,
            @NonNull FilterChain filterChain
    ) throws ServletException, IOException {

        final String authHeader = request.getHeader("Authorization");
        final String jwt;
        final String userEmail;

        // 1. Перевірка: чи є заголовок і чи починається він з "Bearer "
        if (authHeader == null || !authHeader.startsWith("Bearer ")) {
            filterChain.doFilter(request, response); // Якщо ні - передаємо далі
            return;
        }

        jwt = authHeader.substring(7); // Вирізаємо "Bearer "

        try {
            userEmail = jwtService.extractUsername(jwt); // Витягуємо email з токена

            // 2. Перевірка: чи є email і чи *ще немає* автентифікації в SecurityContext
            if (userEmail != null && SecurityContextHolder.getContext().getAuthentication() == null) {

                // 3. Завантажуємо UserDetails (йдемо в базу через наш userDetailsService)
                UserDetails userDetails = this.userDetailsService.loadUserByUsername(userEmail);

                // 4. Перевіряємо валідність токена
                if (jwtService.isTokenValid(jwt, userDetails)) {
                    // 5. ❗️ СТВОРЮЄМО "ТУРНІКЕТ" - кладемо юзера в SecurityContext
                    UsernamePasswordAuthenticationToken authToken = new UsernamePasswordAuthenticationToken(
                            userDetails,
                            null, // Пароль нам тут не потрібен
                            userDetails.getAuthorities()
                    );
                    authToken.setDetails(
                            new WebAuthenticationDetailsSource().buildDetails(request)
                    );
                    // Оновлюємо SecurityContext
                    SecurityContextHolder.getContext().setAuthentication(authToken);
                    log.info("User {} authenticated successfully", userEmail);
                }
            }
        } catch (Exception e) {
            log.warn("Cannot set user authentication: {}", e.getMessage());
        }

        // 6. Передаємо запит далі по ланцюжку фільтрів
        filterChain.doFilter(request, response);
    }
}