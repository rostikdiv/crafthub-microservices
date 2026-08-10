package com.milhub.cart_service.entity;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.util.UUID;

/**
 * Represents a single product item within a cart.
 * Contains cached product details to improve read performance.
 */
@Data
@AllArgsConstructor
@NoArgsConstructor
public class CartItem {

    private UUID productId;
    private String productName; // Cached product name
    private String productImageUrl; // Cached product image URL

    private Integer quantity;
    private BigDecimal price; // Price at the time of addition or last sync

    /**
     * Calculates the subtotal for this specific item (price * quantity).
     *
     * @return The subtotal as a BigDecimal. Returns ZERO if price or quantity is
     *         missing.
     */
    public BigDecimal getSubTotal() {
        if (price == null || quantity == null)
            return BigDecimal.ZERO;
        return price.multiply(BigDecimal.valueOf(quantity));
    }
}