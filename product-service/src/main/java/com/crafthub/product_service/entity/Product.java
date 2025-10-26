package com.crafthub.product_service.entity;

import jakarta.persistence.*;
import lombok.*;
import java.math.BigDecimal; // Використовуємо BigDecimal для грошей!

@Entity
@Table(name = "products")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class Product {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private String name;

    private String description;

    @Column(nullable = false)
    private BigDecimal price; // 💰 Завжди BigDecimal для фінансів

    @Column(nullable = false)
    private Integer stockQuantity; // Кількість на складі
}