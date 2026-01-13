package com.crafthub.order_service.controller;

import com.crafthub.order_service.dto.order.OrderRequestDTO;
import com.crafthub.order_service.dto.order.OrderResponseDTO;
import com.crafthub.order_service.dto.payment.PaymentResponseDTO;
import com.crafthub.order_service.service.OrderService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/orders")
@RequiredArgsConstructor
public class OrderController {

    private final OrderService orderService;

    @PostMapping
    public ResponseEntity<PaymentResponseDTO> createOrder(@RequestBody OrderRequestDTO request) {
        // Тепер ми повертаємо об'єкт з URL для оплати
        return ResponseEntity.ok(orderService.createOrder(request));
    }

    @GetMapping
    public ResponseEntity<List<OrderResponseDTO>> getAllOrders() {
        return ResponseEntity.ok(orderService.getAllOrders());
    }
    // ✅ Додаємо GET метод для конкретного замовлення
    @GetMapping("/{id}")
    public ResponseEntity<OrderResponseDTO> getOrderById(@PathVariable UUID id) {
        return ResponseEntity.ok(orderService.getOrderById(id));
    }
}