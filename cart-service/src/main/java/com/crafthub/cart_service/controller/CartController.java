package com.crafthub.cart_service.controller;

import com.crafthub.cart_service.dto.CartItemRequestDTO;
import com.crafthub.cart_service.model.Cart;
import com.crafthub.cart_service.service.CartService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/cart") // ❗️ Наш базовий URL
@RequiredArgsConstructor
public class CartController {

    private final CartService cartService;

    /**
     * Отримує кошик поточного користувача (або створює новий).
     */
    @GetMapping("/my-cart")
    public ResponseEntity<Cart> getMyCart(
            @RequestHeader("X-User-Email") String userEmail // ❗️ Отримуємо з Gateway
    ) {
        return ResponseEntity.ok(cartService.getCart(userEmail));
    }

    /**
     * Додає/оновлює товар у кошику.
     */
    @PostMapping("/my-cart/items")
    public ResponseEntity<Cart> addItemToMyCart(
            @RequestHeader("X-User-Email") String userEmail,
            @Valid @RequestBody CartItemRequestDTO itemRequest
    ) {
        return ResponseEntity.ok(cartService.addItemToCart(userEmail, itemRequest));
    }

    /**
     * Видаляє товар з кошика.
     */
    @DeleteMapping("/my-cart/items/{productId}")
    public ResponseEntity<Cart> removeItemFromMyCart(
            @RequestHeader("X-User-Email") String userEmail,
            @PathVariable String productId
    ) {
        return ResponseEntity.ok(cartService.removeItemFromCart(userEmail, productId));
    }

    @GetMapping("/test")
    public ResponseEntity<String> test() {
        return ResponseEntity.ok("Test");
    }
}