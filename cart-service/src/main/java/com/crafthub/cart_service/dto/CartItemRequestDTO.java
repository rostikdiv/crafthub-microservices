package com.crafthub.cart_service.dto;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotEmpty;

import java.util.UUID;

/**
 * Data Transfer Object for adding or updating an item in the cart.
 */
public record CartItemRequestDTO(
        @NotEmpty(message = "Product ID is required") UUID productId,

        @Min(value = 1, message = "Quantity must be at least 1") Integer quantity) {
}