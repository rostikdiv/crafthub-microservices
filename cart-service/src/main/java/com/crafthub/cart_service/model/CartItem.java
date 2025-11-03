package com.crafthub.cart_service.model;

import lombok.*;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class CartItem {
    // ❗️ Немає @Id, оскільки це буде вбудований об'єкт
    private String productId;
    private int quantity;
}