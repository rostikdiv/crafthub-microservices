package com.milhub.product_service.entity;

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
@ToString(onlyExplicitlyIncluded = true)
public class Category {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @ToString.Include
    private Long id;

    @Column(nullable = false, unique = true)
    @ToString.Include
    private String name;

    @Column(columnDefinition = "TEXT")
    @ToString.Include
    private String description;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "parent_id")
    private Category parent;

    @OneToMany(mappedBy = "parent", cascade = CascadeType.ALL)
    private List<Category> subCategories;

    @OneToMany(mappedBy = "category", fetch = FetchType.LAZY)
    private List<Product> products;

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != org.hibernate.Hibernate.getClass(o)) return false;
        Category category = (Category) o;
        return getId() != null && getId().equals(category.getId());
    }

    @Override
    public int hashCode() {
        return getClass().hashCode();
    }
}