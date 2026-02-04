package com.crafthub.user_service.entity;

import jakarta.persistence.*;
import lombok.*;
import java.time.LocalDateTime;
import java.util.UUID;

@Entity
@Table(name = "seller_reviews")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class SellerReview {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(nullable = false)
    private UUID sellerId; // ID Продавця (на кого пишуть відгук)

    @Column(nullable = false)
    private UUID userId;   // Автор відгуку

    private String userName; // Ім'я автора (кешуємо, щоб не шукати щоразу)

    private Integer rating; // 1-5

    @Column(columnDefinition = "TEXT")
    private String comment;

    // Відповідь продавця (опціонально, спрощена схема без дерева)
    @Column(columnDefinition = "TEXT")
    private String sellerReply;

    private LocalDateTime createdAt;

    @PrePersist
    protected void onCreate() {
        this.createdAt = LocalDateTime.now();
    }
}