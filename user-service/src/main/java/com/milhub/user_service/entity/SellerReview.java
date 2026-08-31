package com.milhub.user_service.entity;

import jakarta.persistence.*;
import lombok.*;
import java.time.LocalDateTime;
import java.util.UUID;

/**
 * Entity representing a review left for a seller by a buyer.
 */
@Entity
@Table(name = "seller_reviews")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@ToString(onlyExplicitlyIncluded = true)
public class SellerReview {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @ToString.Include
    private UUID id;

    @Column(nullable = false)
    @ToString.Include
    private UUID sellerId;

    @Column(nullable = false)
    @ToString.Include
    private UUID userId;

    @ToString.Include
    private String userName;

    @ToString.Include
    private Integer rating;

    @Column(columnDefinition = "TEXT")
    private String comment;

    @Column(columnDefinition = "TEXT")
    private String sellerReply;

    @ToString.Include
    private LocalDateTime createdAt;

    @PrePersist
    protected void onCreate() {
        this.createdAt = LocalDateTime.now();
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != org.hibernate.Hibernate.getClass(o)) return false;
        SellerReview that = (SellerReview) o;
        return getId() != null && getId().equals(that.getId());
    }

    @Override
    public int hashCode() {
        return getClass().hashCode();
    }
}