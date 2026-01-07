package com.crafthub.order_service.controller;

import com.crafthub.order_service.dto.OrderRequestDTO;
import com.crafthub.order_service.service.OrderService;
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
    public ResponseEntity<String> createOrder(@RequestBody OrderRequestDTO request) {
        // Ми просто передаємо DTO.
        // Вся магія з токеном (User ID, Role) тепер відбувається всередині сервісу.
        String orderId = orderService.createOrder(request);

        return ResponseEntity.status(HttpStatus.CREATED).body(orderId);
    }
}