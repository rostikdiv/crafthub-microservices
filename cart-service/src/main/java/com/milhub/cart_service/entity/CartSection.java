package com.milhub.cart_service.entity;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

/**
 * Represents a group of cart items belonging to a specific seller.
 */
@Data
@AllArgsConstructor
@NoArgsConstructor
public class CartSection {

    private UUID sellerId;
    private String sellerName;
    private String sellerLogoUrl;

    /**
     * List of products in this section.
     */
    private List<CartItem> items = new ArrayList<>();
}