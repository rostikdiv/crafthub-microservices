package com.crafthub.order_service.controller;

import com.crafthub.order_service.dto.OrderRequestDTO;
import com.crafthub.order_service.model.Order;
import com.crafthub.order_service.service.OrderService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/orders")
@RequiredArgsConstructor
public class OrderController {

    private final OrderService orderService;

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public ResponseEntity<Order> createOrder(
            // ❗️ Вмикаємо валідацію DTO
            @Valid @RequestBody OrderRequestDTO orderRequest,

            // ❗️ Отримуємо email користувача з заголовка,
            // який додав наш api-gateway
            @RequestHeader("X-User-Email") String userEmail
    ) {
        Order createdOrder = orderService.createOrder(orderRequest, userEmail);
        return ResponseEntity.status(HttpStatus.CREATED).body(createdOrder);
    }

    // (Тут ми додамо GET /api/v1/orders/my-orders пізніше)
}