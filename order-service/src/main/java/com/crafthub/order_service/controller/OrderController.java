package com.crafthub.order_service.controller;

import com.crafthub.order_service.dto.order.OrderRequestDTO;
import com.crafthub.order_service.dto.order.OrderResponseDTO;
import com.crafthub.order_service.dto.payment.PaymentResponseDTO;
import com.crafthub.order_service.service.OrderService;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/orders")
@RequiredArgsConstructor
public class OrderController {

    private final OrderService orderService;

    @PostMapping
    @PreAuthorize("hasAuthority('order:create')")
    public ResponseEntity<PaymentResponseDTO> createOrder(@RequestBody OrderRequestDTO request) {
        return ResponseEntity.ok(orderService.createOrder(request));
    }

    @GetMapping("/my")
    @PreAuthorize("hasAuthority('order:read:my')")
    public ResponseEntity<Page<OrderResponseDTO>> getMyOrders(
            @PageableDefault(size = 10, sort = "createdAt", direction = Sort.Direction.DESC) Pageable pageable
    ) {
        return ResponseEntity.ok(orderService.getMyOrders(pageable));
    }

    @GetMapping
    @PreAuthorize("hasAuthority('order:read:all')")
    public ResponseEntity<List<OrderResponseDTO>> getAllOrders() {
        return ResponseEntity.ok(orderService.getAllOrders());
    }

    @GetMapping("/{id}")
    public ResponseEntity<OrderResponseDTO> getOrderById(@PathVariable UUID id) {
        return ResponseEntity.ok(orderService.getOrderById(id));
    }

    @GetMapping("/check-purchase")
    public ResponseEntity<Boolean> checkPurchase(@RequestParam UUID productId) {
        return ResponseEntity.ok(orderService.hasUserPurchasedProduct(productId));
    }
}