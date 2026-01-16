package com.crafthub.cart_service.entity;

import lombok.*;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

@Document(collection = "carts")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Cart {

    @Id
    private UUID userId;

    @Builder.Default
    private List<CartSection> sections = new ArrayList<>();

    @Builder.Default
    private BigDecimal totalPrice = BigDecimal.ZERO;
}