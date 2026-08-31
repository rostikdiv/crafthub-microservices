package com.milhub.product_service.entity;

import jakarta.persistence.*;
import lombok.*;
import com.fasterxml.jackson.annotation.JsonIgnore;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

/**
 * Entity representing a product review or a reply to a review.
 */
@Entity
@Table(name = "product_reviews")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@ToString(onlyExplicitlyIncluded = true)
public class ProductReview {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @ToString.Include
    private UUID id;

    @Column(nullable = false)
    @ToString.Include
    private UUID productId; // Link to the product (loose coupling, no @ManyToOne in Product)

    @Column(nullable = false)
    @ToString.Include
    private UUID userId;

    @ToString.Include
    private String userName; // Cached user name
    private String userAvatarUrl; // Optional avatar URL

    @ToString.Include
    private Integer rating; // Rating (1-5). May be null for replies.

    @Column(columnDefinition = "TEXT")
    private String comment;

    @ToString.Include
    private boolean isVerifiedPurchase;

    // SELF-REFERENCING FOR THREADS (Replies)

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "parent_id")
    @JsonIgnore
    private ProductReview parent; // Parent review if this is a reply

    @OneToMany(mappedBy = "parent", cascade = CascadeType.ALL, orphanRemoval = true)
    @OrderBy("createdAt ASC") // Sort replies chronologically
    @Builder.Default
    private List<ProductReview> replies = new ArrayList<>();

    @ToString.Include
    private LocalDateTime createdAt;

    @PrePersist
    protected void onCreate() {
        this.createdAt = LocalDateTime.now();
    }

    // Helper method
    public void addReply(ProductReview reply) {
        replies.add(reply);
        reply.setParent(this);
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != org.hibernate.Hibernate.getClass(o)) return false;
        ProductReview that = (ProductReview) o;
        return getId() != null && getId().equals(that.getId());
    }

    @Override
    public int hashCode() {
        return getClass().hashCode();
    }
}