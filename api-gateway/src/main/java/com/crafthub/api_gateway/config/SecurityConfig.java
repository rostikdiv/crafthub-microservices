package com.crafthub.api_gateway.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.web.reactive.EnableWebFluxSecurity;
import org.springframework.security.config.web.server.ServerHttpSecurity;
import org.springframework.security.web.server.SecurityWebFilterChain;

@Configuration
@EnableWebFluxSecurity // ❗️ Важливо: це WebFlux-версія @EnableWebSecurity
public class SecurityConfig {

    @Bean
    public SecurityWebFilterChain securityWebFilterChain(ServerHttpSecurity http) {
        http
                // 1. Вимикаємо CSRF (не потрібен для stateless API)
                .csrf(ServerHttpSecurity.CsrfSpec::disable)

                // 2. Вимикаємо стандартну форму логіну
                .formLogin(ServerHttpSecurity.FormLoginSpec::disable)

                // 3. Вимикаємо httpBasic
                .httpBasic(ServerHttpSecurity.HttpBasicSpec::disable)

                // 4. Налаштовуємо авторизацію
                .authorizeExchange(exchanges -> exchanges
                        // Дозволяємо всім доступ до наших "публічних" ендпоінтів
                        .pathMatchers("/api/v1/auth/**").permitAll()
                        // (Тут можна додати інші публічні шляхи, наприклад GET /api/v1/products)

                        // Всі інші шляхи вимагають автентифікації
                        .anyExchange().authenticated()
                );

        // ❗️ Ми не додаємо .sessionManagement(STATELESS),
        // тому що наш `AuthenticationFilter` вже робить систему stateless,
        // а Spring Security в WebFlux автоматично не створює сесії,
        // якщо ми не використовуємо formLogin.

        return http.build();
    }
}