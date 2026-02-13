package com.crafthub.user_service.config;

import com.crafthub.user_service.config.filter.HeaderAuthenticationFilter;
import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.authentication.AuthenticationProvider;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;

@Configuration
@EnableWebSecurity
@EnableMethodSecurity // Дозволяє працювати @PreAuthorize
@RequiredArgsConstructor
public class SecurityConfig {

        // ❌ Ми прибрали JwtAuthenticationFilter, бо тепер валідація на Gateway
        private final AuthenticationProvider authenticationProvider;

        @Bean
        public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
                http
                                .csrf(csrf -> csrf.disable()) // Вимикаємо CSRF для PATCH/POST запитів
                                .authorizeHttpRequests(auth -> auth
                                                // Публічні ендпоінти (логін/реєстрація)
                                                .requestMatchers("/api/v1/auth/**").permitAll()
                                                // Swagger UI
                                                .requestMatchers("/swagger-ui/**", "/v3/api-docs/**",
                                                                "/swagger-resources/**", "/webjars/**")
                                                .permitAll()

                                                // 🔥 ВАЖЛИВО: Ми прибрали .hasAuthority("ROLE_ADMIN")
                                                // Тепер ми просто кажемо: "Будь-хто аутентифікований може спробувати
                                                // зайти"
                                                // А вже контролер через @PreAuthorize вирішить, чи є у нього право
                                                // 'user:verify'
                                                .anyRequest().authenticated())
                                .sessionManagement(session -> session
                                                .sessionCreationPolicy(SessionCreationPolicy.STATELESS))
                                .authenticationProvider(authenticationProvider)
                                // Додаємо наш HeaderAuthenticationFilter, який читає заголовки від Gateway
                                .addFilterBefore(new HeaderAuthenticationFilter(),
                                                UsernamePasswordAuthenticationFilter.class);

                return http.build();
        }
}