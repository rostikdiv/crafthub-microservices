package com.crafthub.api_gateway.config;

import org.springframework.cloud.gateway.server.mvc.handler.GatewayRouterFunctions;
import org.springframework.cloud.gateway.server.mvc.handler.HandlerFunctions;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.function.RequestPredicates;
import org.springframework.web.servlet.function.RouterFunction;
import org.springframework.web.servlet.function.ServerResponse;

import java.net.URI;

@Configuration
public class GatewayRouteConfig {

    @Bean
    public RouterFunction<ServerResponse> userServiceRoute() {
        // !!! ТИМЧАСОВА ЗМІНА: Замість пошуку по імені, вказуємо пряму адресу !!!
        URI user_service_uri = URI.create("http://localhost:8081");

        return GatewayRouterFunctions.route("user_service_direct_route") // Зміни ID, щоб не було конфлікту
                .route(RequestPredicates.path("/api/v1/users/**")
                                .or(RequestPredicates.path("/api/v1/auth/**")),
                        HandlerFunctions.http(user_service_uri)) // <-- ПЕРЕДАЄМО ПРЯМЕ URI
                .build();
    }

    @Bean
    public RouterFunction<ServerResponse> productServiceRoute() {
        return GatewayRouterFunctions.route("product-service") // Назва сервісу (LOWERCASE, як планували)
                .route(RequestPredicates.path("/api/v1/products/**"), // Предикат шляху
                        HandlerFunctions.http()) // Перенаправлення HTTP запиту
                .build();
    }

    // Можна додати інші маршрути тут як окремі @Bean методи
}