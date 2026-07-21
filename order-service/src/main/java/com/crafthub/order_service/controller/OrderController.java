package com.crafthub.order_service.controller;

import com.crafthub.order_service.dto.order.OrderRequestDTO;
import com.crafthub.order_service.dto.order.OrderResponseDTO;
import com.crafthub.order_service.dto.payment.PaymentResponseDTO;
import com.crafthub.order_service.entity.OrderStatus;
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

/**
 * REST controller for managing orders.
 * Handles order creation, cancellation, returns, and status updates.
 */
@RestController
@RequestMapping("/api/v1/orders")
@RequiredArgsConstructor
public class OrderController {

    private final OrderService orderService;
    // private final com.crafthub.order_service.service.ReturnService returnService;
    // // Removed

    /**
     * Cancels an order by its ID.
     *
     * @param id     The ID of the order to cancel.
     * @param reason The reason for cancellation (optional).
     * @return A ResponseEntity with no content.
     */
    @PostMapping("/{id}/cancel")
    @PreAuthorize("hasAuthority('order:create')")
    public ResponseEntity<Void> cancelOrder(
            @PathVariable UUID id,
            @RequestBody(required = false) String reason) { // Reason is optional
        orderService.cancelMyOrder(id, reason);
        return ResponseEntity.ok().build();
    }

    /**
     * Creates a new order.
     *
     * @param request The order request DTO containing order details.
     * @return A ResponseEntity containing the payment response DTO.
     */
    @PostMapping
    @PreAuthorize("hasAuthority('order:create')")
    public ResponseEntity<PaymentResponseDTO> createOrder(@RequestBody OrderRequestDTO request) {
        return ResponseEntity.ok(orderService.createOrder(request));
    }

    /**
     * Requests a return for a specific order.
     *
     * @param id      The ID of the order for which to request a return.
     * @param request The simple return request containing the reason.
     * @return A ResponseEntity with no content.
     */
    @PostMapping("/{id}/return")
    @PreAuthorize("hasAuthority('order:create')")
    public ResponseEntity<Void> requestReturn(
            @PathVariable UUID id,
            @RequestBody SimpleReturnRequest request) {
        orderService.requestReturn(id, request.reason());
        return ResponseEntity.ok().build();
    }

    /**
     * Processes a return request for a specific order.
     *
     * @param id       The ID of the order whose return is being processed.
     * @param approved A boolean indicating whether the return is approved or not.
     * @return A ResponseEntity with no content.
     */
    @PutMapping("/{id}/return/process")
    @PreAuthorize("hasRole('SELLER')")
    public ResponseEntity<Void> processReturn(
            @PathVariable UUID id,
            @RequestParam boolean approved) {
        orderService.processReturn(id, approved);
        return ResponseEntity.ok().build();
    }

    /**
     * Completes a return process for a specific order.
     *
     * @param id The ID of the order whose return is being completed.
     * @return A ResponseEntity with no content.
     */
    @PutMapping("/{id}/return/complete")
    @PreAuthorize("hasRole('SELLER')")
    public ResponseEntity<Void> completeReturn(
            @PathVariable UUID id) {
        orderService.completeReturn(id);
        return ResponseEntity.ok().build();
    }

    public record SimpleReturnRequest(String reason) {
    }

    @GetMapping("/my")
    @PreAuthorize("hasAuthority('order:read:my')")
    public ResponseEntity<Page<OrderResponseDTO>> getMyOrders(
            @PageableDefault(size = 10, sort = "createdAt", direction = Sort.Direction.DESC) Pageable pageable) {
        return ResponseEntity.ok(orderService.getMyOrders(pageable));
    }

    @GetMapping
    @PreAuthorize("hasAuthority('order:read:all')")
    public ResponseEntity<Page<OrderResponseDTO>> getAllOrders(
            @PageableDefault(size = 10, sort = "createdAt", direction = Sort.Direction.DESC) Pageable pageable) {
        return ResponseEntity.ok(orderService.getAllOrders(pageable));
    }

    @GetMapping("/{id}")
    public ResponseEntity<OrderResponseDTO> getOrderById(@PathVariable UUID id) {
        return ResponseEntity.ok(orderService.getOrderById(id));
    }

    @GetMapping("/check-purchase")
    public ResponseEntity<Boolean> checkPurchase(@RequestParam UUID productId) {
        return ResponseEntity.ok(orderService.hasUserPurchasedProduct(productId));
    }

    @GetMapping("/seller")
    @PreAuthorize("hasRole('SELLER')")
    public ResponseEntity<Page<OrderResponseDTO>> getSellerOrders(
            @RequestParam(required = false) OrderStatus status,
            @PageableDefault(size = 10, sort = "createdAt", direction = Sort.Direction.DESC) Pageable pageable) {
        return ResponseEntity.ok(orderService.getSellerOrders(status, pageable));
    }

    @PatchMapping("/{id}/status")
    @PreAuthorize("hasRole('SELLER')")
    public ResponseEntity<OrderResponseDTO> updateOrderStatus(
            @PathVariable UUID id,
            @RequestParam OrderStatus status) {
        return ResponseEntity.ok(orderService.updateOrderStatusBySeller(id, status));
    }

    @GetMapping("/check-seller-purchase")
    public ResponseEntity<Boolean> checkSellerPurchase(
            @RequestParam UUID userId,
            @RequestParam UUID sellerId) {
        return ResponseEntity.ok(orderService.hasUserBoughtFromSeller(userId, sellerId));
    }
}