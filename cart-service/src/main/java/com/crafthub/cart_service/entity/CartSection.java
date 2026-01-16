package com.crafthub.cart_service.entity;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class CartSection {

    private UUID sellerId;
    private String sellerName;
    private String sellerLogoUrl;

    private List<CartItem> items = new ArrayList<>();
}