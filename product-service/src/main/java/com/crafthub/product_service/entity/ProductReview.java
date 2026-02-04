package com.crafthub.product_service.entity;

import jakarta.persistence.*;
import lombok.*;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

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
    private UUID productId; // Зв'язок з товаром (без @ManyToOne у Product)

    @Column(nullable = false)
    private UUID userId;

    private String userName; // Кешуємо ім'я
    private String userAvatarUrl; // Можна додати аватарку

    private Integer rating; // Рейтинг (1-5). Для відповідей може бути null.

    @Column(columnDefinition = "TEXT")
    private String comment;

    private boolean isVerifiedPurchase;

    // 🔥 РЕАЛІЗАЦІЯ ГІЛОК (Self-Referencing)

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "parent_id")
    private ProductReview parent; // Батьківський коментар

    @OneToMany(mappedBy = "parent", cascade = CascadeType.ALL, orphanRemoval = true)
    @OrderBy("createdAt ASC") // Відповіді сортуємо хронологічно
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