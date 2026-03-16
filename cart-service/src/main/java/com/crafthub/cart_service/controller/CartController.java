package com.crafthub.cart_service.controller;

import com.crafthub.cart_service.dto.CartItemRequestDTO;
import com.crafthub.cart_service.entity.Cart;
import com.crafthub.cart_service.service.CartService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

/**
 * REST controller for managing the shopping cart.
 * Provides endpoints for retrieving, adding items to, and clearing the cart.
 */
@RestController
@RequestMapping("/api/v1/cart")
@RequiredArgsConstructor
public class CartController {

    private final CartService cartService;

    /**
     * Retrieves the current user's shopping cart.
     * User ID is automatically extracted from the security context.
     *
     * @return A ResponseEntity containing the user's Cart.
     */
    @GetMapping
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<Cart> getCart() {
        return ResponseEntity.ok(cartService.getMyCart());
    }

    /**
     * Adds an item to the current user's shopping cart.
     *
     * @param itemDto The details of the item to add.
     * @return A ResponseEntity containing the updated Cart.
     */
    @PostMapping("/items")
    @PreAuthorize("hasAuthority('order:create')")
    public ResponseEntity<Cart> addItem(@RequestBody CartItemRequestDTO itemDto) {
        return ResponseEntity.ok(cartService.addItemToMyCart(itemDto));
    }

    /**
     * Removes a specific item from the current user's shopping cart.
     *
     * @param productId The ID of the product to remove.
     * @return A ResponseEntity containing the updated Cart.
     */
    @DeleteMapping("/items/{productId}")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<Cart> removeItem(@PathVariable String productId) {
        return ResponseEntity.ok(cartService.removeItemFromMyCart(productId));
    }

    /**
     * Clears all items from the current user's shopping cart.
     *
     * @return An empty ResponseEntity with No Content status.
     */
    @DeleteMapping
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<Void> clearCart() {
        cartService.clearMyCart();
        return ResponseEntity.noContent().build();
    }

    /**
     * Simple test endpoint to verify service availability.
     *
     * @return A test message.
     */
    @GetMapping("/test")
    public ResponseEntity<String> test() {
        return ResponseEntity.ok("cart service works!");
    }
}