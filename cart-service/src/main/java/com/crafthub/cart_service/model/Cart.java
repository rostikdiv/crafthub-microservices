package com.crafthub.cart_service.model;

import lombok.*;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;

import java.util.ArrayList;
import java.util.List;

@Document(collection = "carts") // ❗️ Вказуємо назву колекції в MongoDB
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Cart {

    @Id // ❗️ Ми будемо використовувати email користувача як унікальний ID
    private String userId;

    @Builder.Default // ❗️ За замовчуванням - порожній список
    private List<CartItem> items = new ArrayList<>();
}