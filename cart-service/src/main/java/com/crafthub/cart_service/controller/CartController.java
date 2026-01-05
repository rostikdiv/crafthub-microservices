package com.crafthub.cart_service.controller;

import com.crafthub.cart_service.dto.CartItemRequestDTO;
import com.crafthub.cart_service.entity.Cart;
import com.crafthub.cart_service.security.JwtParserService;
import com.crafthub.cart_service.service.CartService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

@RestController
@RequestMapping("/api/v1/cart") // ❗️ Наш базовий URL
@RequiredArgsConstructor
public class CartController {

    private final CartService cartService;
    private final JwtParserService jwtService;

    /**
     * Отримує кошик поточного користувача (або створює новий).
     */
    @GetMapping
    public ResponseEntity<Cart> getCart(@RequestHeader("Authorization") String token) {
        UUID userId = jwtService.extractUserId(token); // ✅ ID з токена
        return ResponseEntity.ok(cartService.getCart(userId));
    }
    /**
     * Додає/оновлює товар у кошику.
     */
    @PostMapping("/items")
    public ResponseEntity<Cart> addItem(@RequestHeader("Authorization") String token,
                                        @RequestBody CartItemRequestDTO itemDto) {
        UUID userId = jwtService.extractUserId(token); // ✅ ID з токена
        return ResponseEntity.ok(cartService.addItemToCart(userId, itemDto));
    }

    /**
     * Видаляє товар з кошика.
     */
    @DeleteMapping("/items/{productId}")
    public ResponseEntity<Cart> removeItem(@RequestHeader("Authorization") String token,
                                           @PathVariable String productId) {
        UUID userId = jwtService.extractUserId(token); // ✅ ID з токена
        return ResponseEntity.ok(cartService.removeItemFromCart(userId, productId));
    }

    @GetMapping("/test")
    public ResponseEntity<String> test() {
        return ResponseEntity.ok("cart service works!");
    }
}