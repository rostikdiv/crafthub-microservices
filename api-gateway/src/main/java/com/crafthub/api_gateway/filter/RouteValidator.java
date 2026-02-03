package com.crafthub.api_gateway.filter;

import org.springframework.http.HttpMethod;
import org.springframework.http.server.reactive.ServerHttpRequest;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.function.Predicate;

@Component
public class RouteValidator {

    // 1. Маршрути, які відкриті ЗАВЖДИ (незалежно від методу запиту)
    public static final List<String> openApiEndpoints = List.of(
            "/api/v1/auth/register",
            "/api/v1/auth/authenticate",
            "/api/v1/delivery/locations", // Пошук міст зазвичай публічний
            "/eureka"
    );

    public Predicate<ServerHttpRequest> isSecured = request -> {
        String path = request.getURI().getPath();

        // ЕТАП 1: Перевірка повністю відкритих маршрутів (Auth, Register...)
        // Якщо шлях є в списку openApiEndpoints -> повертаємо false (НЕ захищений)
        if (openApiEndpoints.stream().anyMatch(path::contains)) {
            return false;
        }

        // ЕТАП 2: Спеціальна логіка для Товарів та Категорій
        // Дозволяємо доступ без токена ТІЛЬКИ для методу GET
        if (request.getMethod().equals(HttpMethod.GET)) {
            // Якщо це GET запит на продукти або категорії -> пускаємо без токена
            if (path.contains("/api/v1/products") || path.contains("/api/v1/categories")) {
                return false; // Не захищений
            }
        }

        // ЕТАП 3: Все інше за замовчуванням захищене (Cart, Orders, Admin, POST-запити на товари...)
        return true;
    };
}