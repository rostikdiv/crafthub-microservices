package com.crafthub.cart_service.controller;

import com.crafthub.cart_service.dto.CartItemRequestDTO;
import com.crafthub.cart_service.entity.Cart;
import com.crafthub.cart_service.service.CartService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/cart")
@RequiredArgsConstructor
public class CartController {

    private final CartService cartService;
    /**
     * Отримує кошик поточного користувача.
     * ID береться автоматично всередині cartService.getMyCart()
     */
    @GetMapping
    public ResponseEntity<Cart> getCart() {
        return ResponseEntity.ok(cartService.getMyCart());
    }

    /**
     * Додає товар у кошик поточного користувача.
     */
    @PostMapping("/items")
    public ResponseEntity<Cart> addItem(@RequestBody CartItemRequestDTO itemDto) {
        return ResponseEntity.ok(cartService.addItemToMyCart(itemDto));
    }

    /**
     * Видаляє товар з кошика поточного користувача.
     */
    @DeleteMapping("/items/{productId}")
    public ResponseEntity<Cart> removeItem(@PathVariable String productId) {
        return ResponseEntity.ok(cartService.removeItemFromMyCart(productId));
    }

    /**
     * Очищає весь кошик поточного користувача.
     */
    @DeleteMapping
    public ResponseEntity<Void> clearCart() {
        cartService.clearMyCart();
        return ResponseEntity.noContent().build();
    }

    @GetMapping("/test")
    public ResponseEntity<String> test() {
        return ResponseEntity.ok("cart service works!");
    }
}