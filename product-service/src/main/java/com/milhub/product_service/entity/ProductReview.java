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
public class ProductReview {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(nullable = false)
    private UUID productId; // Link to the product (loose coupling, no @ManyToOne in Product)

    @Column(nullable = false)
    private UUID userId;

    private String userName; // Cached user name
    private String userAvatarUrl; // Optional avatar URL

    private Integer rating; // Rating (1-5). May be null for replies.

    @Column(columnDefinition = "TEXT")
    private String comment;

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
}