package com.crafthub.order_service.controller;

import com.crafthub.order_service.dto.OrderRequestDTO;
import com.crafthub.order_service.service.OrderService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/orders")
@RequiredArgsConstructor
public class OrderController {

    private final OrderService orderService;

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public String createOrder(
            @RequestBody OrderRequestDTO request,
            @RequestHeader("X-User-Id") String userId,
            @RequestHeader("X-User-Role") String userRole,
            @RequestHeader(value = "X-User-Email", required = false) String userEmail // ✅ Читаємо Email
    ) {
        // Якщо email не прийшов (наприклад, прямий запит без Gateway), ставимо заглушку, щоб не впало
        String email = (userEmail != null) ? userEmail : "unknown@mil.ua";

        return orderService.createOrder(request, userId, userRole, email);
    }
}