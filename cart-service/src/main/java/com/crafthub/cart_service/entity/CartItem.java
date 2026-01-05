package com.crafthub.cart_service.entity;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.util.UUID;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class CartItem {

    private UUID productId; // ✅ Змінено на UUID
    private String productName;      // Кешуємо назву
    private String productImageUrl;  // Кешуємо картинку

    private Integer quantity;
    private BigDecimal price; // Ціна на момент додавання (або актуальна)

    // Метод для розрахунку вартості позиції
    public BigDecimal getSubTotal() {
        if (price == null || quantity == null) return BigDecimal.ZERO;
        return price.multiply(BigDecimal.valueOf(quantity));
    }
}