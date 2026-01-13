package com.crafthub.user_service.entity;

import jakarta.persistence.*;
import lombok.*;
import java.util.UUID;

@Entity
@Table(name = "seller_profiles")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class SellerProfile {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @OneToOne
    @JoinColumn(name = "user_id", referencedColumnName = "id")
    private User user;

    @Column(nullable = false)
    private String companyName;

    @Column(columnDefinition = "TEXT")
    private String description;

    private String logoUrl;

    @Column(nullable = false, unique=true)
    private String taxId; // ІПН або ЄДРПОУ

    private Float rating; // Рейтинг продавця

    // ... всередині класу SellerProfile додайте:
    @OneToMany(mappedBy = "sellerProfile", cascade = CascadeType.ALL, orphanRemoval = true)
    private java.util.List<SellerPoint> pickupPoints;
}