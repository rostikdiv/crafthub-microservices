package com.crafthub.product_service.entity;

import jakarta.persistence.*;
import lombok.*;
import java.util.List;

/**
 * Entity representing a product category.
 */
@Entity
@Table(name = "categories")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Category {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY) // ID auto-generation (1, 2, 3...)
    private Long id;

    @Column(nullable = false, unique = true)
    private String name;

    @Column(columnDefinition = "TEXT")
    private String description;

    @OneToMany(mappedBy = "category", fetch = FetchType.LAZY)
    private List<Product> products;
}