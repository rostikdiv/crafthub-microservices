package com.milhub.product_service.entity;

import com.milhub.product_service.entity.enums.AccessLevel;
import jakarta.persistence.*;
import lombok.*;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

/**
 * Entity representing a product in the catalog.
 */
@Entity
@Table(name = "products")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@ToString(onlyExplicitlyIncluded = true)
public class Product {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @ToString.Include
    private UUID id;

    @Column(nullable = false)
    @ToString.Include
    private String name;

    @Column(columnDefinition = "TEXT")
    private String description;

    @Column(nullable = false)
    @ToString.Include
    private BigDecimal price;

    @Column(name = "old_price")
    @ToString.Include
    private BigDecimal oldPrice;

    @Column(nullable = false)
    @ToString.Include
    private Integer quantity;

    @Column(nullable = false)
    private Double weight;

    @Column(nullable = false)
    private Double length;

    @Column(nullable = false)
    private Double width;

    @Column(nullable = false)
    private Double height;

    @Column(nullable = false)
    private String previewImageUrl;

    @ElementCollection(fetch = FetchType.LAZY)
    @CollectionTable(name = "product_images", joinColumns = @JoinColumn(name = "product_id"))
    @Column(name = "image_url")
    private List<String> imageUrls;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    @ToString.Include
    private AccessLevel accessLevel;

    @Column(nullable = false)
    @ToString.Include
    private UUID sellerId;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "category_id", nullable = false)
    private Category category;

    @ToString.Include
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;

    @ToString.Include
    private String sellerName;
    private String sellerLogoUrl;

    @Builder.Default
    @Column(nullable = false, columnDefinition = "double precision default 0.0")
    @ToString.Include
    private Double averageRating = 0.0;

    @Builder.Default
    @Column(nullable = false, columnDefinition = "integer default 0")
    @ToString.Include
    private Integer reviewCount = 0;

    @PrePersist
    protected void onCreate() {
        this.createdAt = LocalDateTime.now();
        this.updatedAt = LocalDateTime.now();
        if (this.accessLevel == null)
            this.accessLevel = AccessLevel.PUBLIC;
    }

    @PreUpdate
    protected void onUpdate() {
        this.updatedAt = LocalDateTime.now();
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != org.hibernate.Hibernate.getClass(o)) return false;
        Product product = (Product) o;
        return getId() != null && getId().equals(product.getId());
    }

    @Override
    public int hashCode() {
        return getClass().hashCode();
    }
}